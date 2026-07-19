package com.umc.linkyou.service.curation.recommend.external;

import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.infra.ai.AiSearchService;
import com.umc.linkyou.infra.ai.dto.ExternalLinkResultDTO;
import com.umc.linkyou.infra.ai.dto.ExternalSearchRequest;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.service.common.EmotionMapper;
import com.umc.linkyou.service.common.ImageFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalRecommendWorker {

    private static final int EXTERNAL_LIMIT = 5;

    private final CurationRepository curationRepository;
    private final CurationLinkuRepository curationLinkuRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final AiSearchService aiSearchService;
    private final ImageFetchService imageFetchService;
    private final EmotionMapper emotionMapper;
    private final SituationRepository situationRepository;

    @Transactional
    public void generateExternal(Long curationId) {
        log.info("[EXT] 외부 추천 시작 curationId={}", curationId);

        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new GeneralException(CurationErrorStatus._CURATION_NOT_FOUND));
        Users user = curation.getUser();
        Long userId = user.getId();

        List<String> topTags = keywordMonthlyCountRepository.findTopByUserIdAndBaseMonth(userId, curation.getBaseMonth(), PageRequest.of(0, 3))
                .stream()
                .map(kmc -> kmc.getType() == KeywordType.EMOTION
                        ? emotionMapper.getEmotionName(kmc.getRefId())
                        : situationRepository.findNameById(kmc.getRefId()).orElse(""))
                .filter(name -> !name.isBlank())
                .toList();

        String jobName = user.getJob() != null ? user.getJob().getName() : null;
        String gender  = user.getGender() != null ? user.getGender().name() : null;

        List<ExternalLinkResultDTO> external;
        try {
            long t0 = System.currentTimeMillis();
            external = aiSearchService.searchExternalLinks(
                    new ExternalSearchRequest(topTags, EXTERNAL_LIMIT, jobName, gender));
            log.info("[AI] 외부 추천 응답 elapsed={}ms", System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.warn("[AI] 외부 추천 실패, 기존 데이터 유지 curationId={}", curationId, e);
            return;
        }

        if (external.isEmpty()) {
            log.warn("[EXT] AI 응답 없음, 기존 데이터 유지 curationId={}", curationId);
            return;
        }

        // 이미지 fetch 병렬 실행
        List<Map.Entry<ExternalLinkResultDTO, CompletableFuture<String>>> tasks = external.stream()
                .filter(item -> item.getUrl() != null && !item.getUrl().isBlank())
                .map(item -> Map.entry(item, imageFetchService.fetchAsync(item.getUrl(), item.getTitle())))
                .toList();

        curationLinkuRepository.deleteAllByCurationIdAndType(curationId, CurationLinkuType.EXTERNAL);

        List<CurationLinku> toSave = tasks.stream()
                .map(entry -> CurationLinku.ofExternal(curation, entry.getKey().getUrl(), entry.getKey().getTitle(), entry.getValue().join()))
                .toList();
        curationLinkuRepository.saveAll(toSave);

        log.info("[EXT] 외부 추천 저장 완료 curationId={}, rows={}", curationId, toSave.size());
    }
}
