package com.umc.linkyou.service.curation.linku;

import com.umc.linkyou.TitleImgParser.LinkToImageService;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.log.CurationTopLog;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.repository.CurationRepository;
import com.umc.linkyou.repository.LogRepository.CurationTopLogRepository;
import com.umc.linkyou.repository.UserRepository;
import com.umc.linkyou.repository.curationLinkuRepository.CurationLinkuRepository;
import com.umc.linkyou.service.curation.perplexity.PerplexityExternalSearchService;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalRecommendWorker {

    private final CurationRepository curationRepository;
    private final CurationLinkuRepository curationLinkuRepository;
    private final InternalLinkCandidateService internalLinkCandidateService;
    private final CurationTopLogRepository curationTopLogRepository;
    private final PerplexityExternalSearchService perplexityExternalSearchService;
    private final UserRepository userRepository;
    private final LinkToImageService linkToImageService; // 저장 시점에만 사용

    /**
     * Perplexity 호출 → curation_linku(type=EXTERNAL)에 url/title/imageUrl 저장
     * 트랜잭션 경계는 이 Worker에서 관리한다.
     */
    @Transactional
    public int generateAndStoreExternal(Long curationId) {
        log.info("[EXT] start materialize curationId={}", curationId);

        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("curation not found"));
        Long userId = curation.getUser().getId();

        // 내부 후보(최근 URL) 4개 → 프롬프트 힌트
        var internalCandidates = internalLinkCandidateService.getInternalCandidates(userId, curationId, 4);
        int externalLimit = Math.max(0, 9 - internalCandidates.size());
        var recentUrls = internalCandidates.stream().map(RecommendedLinkResponse::getUrl).toList();

        // 상위 태그
        var topTags = curationTopLogRepository.findTopTagsByUserId(userId, 3)
                .stream().map(CurationTopLog::getTagName).toList();

        // 사용자 프로필
        var user = userRepository.findById(userId).orElseThrow();
        String jobName = user.getJob() != null ? user.getJob().getName() : null;
        String gender  = user.getGender() != null ? user.getGender().name() : null;

        // Perplexity
        List<RecommendedLinkResponse> external;
        try {
            long t0 = System.currentTimeMillis();
            external = perplexityExternalSearchService.searchExternalLinks(
                    recentUrls, topTags, externalLimit, jobName, gender
            );
            log.info("[Perplexity] elapsed={}ms", System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.warn("[Perplexity] 외부 추천 실패: {}", e.toString());
            external = List.of();
        }

        // 기존 EXTERNAL 캐시 전체 삭제 후 재삽입
        curationLinkuRepository.deleteAllByCurationIdAndType(curationId, CurationLinkuType.EXTERNAL);

        int saved = 0;
        for (var item : external) {
            if (item.getUrl() == null || item.getUrl().isBlank()) continue;

            // 저장 시점에 이미지도 확보(실패 허용)
            String imageUrl = fetchImageUrlFast(item.getUrl());

            curationLinkuRepository.save(
                    CurationLinku.ofExternal(curation, item.getUrl(), item.getTitle(), imageUrl) // ← imageUrl 추가
            );
            saved++;
        }
        log.info("[EXT] saved rows={}", saved);
        return saved;
    }

    /** 이미지 파싱 실패는 무시(성능/안정성 우선) */
    private String fetchImageUrlFast(String url) {
        try {
            return linkToImageService.getRelatedImageFromUrl(url);
        } catch (Exception e) {
            return null;
        }
    }
}
