package com.umc.linkyou.service.curation.linku.external;

import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.infra.ai.gemini.GeminiExternalSearchService;
import com.umc.linkyou.service.common.EmotionTagMapper;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalRecommendWorker {

    private final CurationRepository curationRepository;
    private final CurationLinkuRepository curationLinkuRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final GeminiExternalSearchService geminiExternalSearchService;
    private final LinkToImageService linkToImageService;
    private final EmotionTagMapper emotionTagMapper;
    private final SituationJobRepository situationJobRepository;

    // Gemini AI로 외부 링크를 추천받아, 이미지와 함께 DB에 저장
    @Transactional
    public int generateAndStoreExternal(Long curationId) {
        log.info("[EXT] start materialize curationId={}", curationId);

        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("curation not found"));
        var user = curation.getUser();
        Long userId = user.getId();

        int externalLimit = 5;

        // 큐레이션 대상 월 상위 태그
        var topTags = keywordMonthlyCountRepository.findTopByUserIdAndBaseMonth(userId, curation.getMonth(), PageRequest.of(0, 3))
                .stream()
                .map(kmc -> kmc.getType() == KeywordType.EMOTION
                        ? emotionTagMapper.getEmotionName(kmc.getRefId())
                        : situationJobRepository.findById(kmc.getRefId())
                                .map(sj -> sj.getSituation().getName())
                                .orElse(""))
                .filter(name -> !name.isBlank())
                .toList();

        // 사용자 프로필
        String jobName = user.getJob() != null ? user.getJob().getName() : null;
        String gender  = user.getGender() != null ? user.getGender().name() : null;

        // Gemini
        List<RecommendedLinkResponse> external;
        try {
            long t0 = System.currentTimeMillis();
            external = geminiExternalSearchService.searchExternalLinks(
                    topTags, externalLimit, jobName, gender
            );
            log.info("[Gemini] elapsed={}ms", System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.warn("[Gemini] 외부 추천 실패", e);
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

    // 이미지 파싱 실패는 무시
    private String fetchImageUrlFast(String url) {
        try {
            return linkToImageService.getRelatedImageFromUrl(url);
        } catch (Exception e) {
            return null;
        }
    }
}
