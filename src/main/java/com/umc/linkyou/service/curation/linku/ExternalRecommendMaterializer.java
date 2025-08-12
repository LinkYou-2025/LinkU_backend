package com.umc.linkyou.service.curation.linku;

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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalRecommendMaterializer {

    private final CurationRepository curationRepository;
    private final CurationLinkuRepository curationLinkuRepository;
    private final InternalLinkCandidateService internalLinkCandidateService;
    private final CurationTopLogRepository curationTopLogRepository;
    private final PerplexityExternalSearchService perplexityExternalSearchService;
    private final UserRepository userRepository;
    private final Semaphore externalRecoLimiter;

    /** 비동기 래퍼(권장) */
    @Async("defaultTaskExecutor")
    public void generateAndStoreExternalAsync(Long curationId) {
        try {
            externalRecoLimiter.acquire();
            generateAndStoreExternal(curationId);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("‼️ external recommend interrupted for curationId={}", curationId);
        } finally {
            externalRecoLimiter.release();
        }
    }

    /** Perplexity 호출 → curation_linku(type=EXTERNAL)에 url/title 저장 */
    @Transactional
    public int generateAndStoreExternal(Long curationId) {
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("curation not found"));
        Long userId = curation.getUser().getId();

        // 내부 후보(최근 URL) 4개 → 프롬프트 힌트
        var internalCandidates = internalLinkCandidateService.getInternalCandidates(userId, curationId, 4);
        int externalLimit = Math.max(0, 9 - internalCandidates.size());
        var recentUrls = internalCandidates.stream().map(RecommendedLinkResponse::getUrl).toList();

        // 상위 태그(필요 시)
        var topTags = curationTopLogRepository.findTopTagsByUserId(userId, 3)
                .stream().map(CurationTopLog::getTagName).toList();

        // 사용자 프로필
        var user = userRepository.findById(userId).orElseThrow();
        String jobName = user.getJob()!=null ? user.getJob().getName() : null;
        String gender  = user.getGender()!=null ? user.getGender().name() : null;

        List<RecommendedLinkResponse> external;
        try {
            external = perplexityExternalSearchService.searchExternalLinks(
                    recentUrls, topTags, externalLimit, jobName, gender
            );
        } catch (Exception e) {
            log.warn("[Perplexity] 외부 추천 실패: {}", e.toString());
            external = List.of();
        }

        // 기존 EXTERNAL 캐시 전체 삭제 후 재삽입(단순/안전)
        curationLinkuRepository.deleteAllByCurationIdAndType(curationId, CurationLinkuType.EXTERNAL);

        int saved = 0;
        for (var item : external) {
            if (item.getUrl()==null || item.getUrl().isBlank()) continue;
            curationLinkuRepository.save(CurationLinku.ofExternal(curation, item.getUrl(), item.getTitle()));
            saved++;
        }
        return saved;
    }
}
