package com.umc.linkyou.service.curation.linku.external;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.infra.ai.AiSearchService;
import com.umc.linkyou.infra.ai.dto.ExternalLinkDTO;
import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.service.common.EmotionTagMapper;
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
    private final AiSearchService aiSearchService;
    private final LinkToImageService linkToImageService;
    private final EmotionTagMapper emotionTagMapper;
    private final SituationJobRepository situationJobRepository;

    @Transactional
    public int generateAndStoreExternal(Long curationId) {
        log.info("[EXT] start materialize curationId={}", curationId);

        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("curation not found"));
        var user = curation.getUser();
        Long userId = user.getId();

        int externalLimit = 5;

        var topTags = keywordMonthlyCountRepository.findTopByUserIdAndBaseMonth(userId, curation.getMonth(), PageRequest.of(0, 3))
                .stream()
                .map(kmc -> kmc.getType() == KeywordType.EMOTION
                        ? emotionTagMapper.getEmotionName(kmc.getRefId())
                        : situationJobRepository.findById(kmc.getRefId())
                                .map(sj -> sj.getSituation().getName())
                                .orElse(""))
                .filter(name -> !name.isBlank())
                .toList();

        String jobName = user.getJob() != null ? user.getJob().getName() : null;
        String gender  = user.getGender() != null ? user.getGender().name() : null;

        List<ExternalLinkDTO> external;
        try {
            long t0 = System.currentTimeMillis();
            external = aiSearchService.searchExternalLinks(topTags, externalLimit, jobName, gender);
            log.info("[AI] elapsed={}ms", System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.warn("[AI] 외부 추천 실패", e);
            external = List.of();
        }

        curationLinkuRepository.deleteAllByCurationIdAndType(curationId, CurationLinkuType.EXTERNAL);

        int saved = 0;
        for (var item : external) {
            if (item.getUrl() == null || item.getUrl().isBlank()) continue;

            String imageUrl = fetchImageUrlFast(item.getUrl());

            curationLinkuRepository.save(
                    CurationLinku.ofExternal(curation, item.getUrl(), item.getTitle(), imageUrl)
            );
            saved++;
        }
        log.info("[EXT] saved rows={}", saved);
        return saved;
    }

    private String fetchImageUrlFast(String url) {
        try {
            return linkToImageService.getRelatedImageFromUrl(url);
        } catch (Exception e) {
            return null;
        }
    }
}
