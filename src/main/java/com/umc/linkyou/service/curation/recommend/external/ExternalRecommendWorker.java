package com.umc.linkyou.service.curation.recommend.external;

import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.AiSearchService;
import com.umc.linkyou.infra.ai.dto.ExternalLinkResultDTO;
import com.umc.linkyou.infra.ai.dto.ExternalSearchRequest;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.service.common.EmotionMapper;
import com.umc.linkyou.service.common.ImageFetchService;
import com.umc.linkyou.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalRecommendWorker {

    private static final int EXTERNAL_LIMIT = 5;
    // 도달 불가/봇 차단으로 걸러지는 링크를 대비해 필요한 개수보다 여유 있게 요청한다.
    private static final int EXTERNAL_SEARCH_LIMIT = 8;

    private final CurationRepository curationRepository;
    private final CurationLinkuRepository curationLinkuRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final AiSearchService aiSearchService;
    private final ImageFetchService imageFetchService;
    private final EmotionMapper emotionMapper;
    private final SituationRepository situationRepository;
    private final SafeUrlFetcher safeUrlFetcher;

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
                    new ExternalSearchRequest(topTags, EXTERNAL_SEARCH_LIMIT, jobName, gender));
            log.info("[AI] 외부 추천 응답 elapsed={}ms", System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.warn("[AI] 외부 추천 실패, 기존 데이터 유지 curationId={}", curationId, e);
            return;
        }

        if (external.isEmpty()) {
            log.warn("[EXT] AI 응답 없음, 기존 데이터 유지 curationId={}", curationId);
            return;
        }

        // 도달 불가/봇 차단 링크는 즉시 탈락시킨다.
        List<ExternalLinkResultDTO> reachable = external.stream()
                .filter(item -> item.getUrl() != null && !item.getUrl().isBlank())
                .filter(item -> safeUrlFetcher.isReachable(item.getUrl()))
                .toList();

        if (reachable.isEmpty()) {
            log.warn("[EXT] 도달 가능한 링크 없음, 기존 데이터 유지 curationId={}", curationId);
            return;
        }

        // 유저가 이미 저장한 링크는 외부 추천 후보에서 제외하고, 나머지 후보로 목표 개수(EXTERNAL_LIMIT)를 채운다.
        // 저장된 Linku.linkuUrl은 정규화된 값이므로 비교 전 동일하게 정규화한다.
        List<String> normalizedUrls = reachable.stream()
                .map(item -> UrlUtils.normalizeUrl(item.getUrl()))
                .toList();
        List<UsersLinku> savedMatches = usersLinkuRepository.findByUserIdAndLinkuUrlIn(userId, normalizedUrls);
        Set<String> savedUrls = savedMatches.stream()
                .map(ul -> ul.getLinku().getLinkuUrl())
                .collect(Collectors.toSet());

        List<ExternalLinkResultDTO> candidates = reachable.stream()
                .filter(item -> !savedUrls.contains(UrlUtils.normalizeUrl(item.getUrl())))
                .limit(EXTERNAL_LIMIT)
                .toList();

        if (candidates.isEmpty()) {
            log.warn("[EXT] 저장되지 않은 도달 가능 링크 없음, 기존 데이터 유지 curationId={}", curationId);
            return;
        }

        // 이미지 fetch 병렬 실행
        List<Map.Entry<ExternalLinkResultDTO, CompletableFuture<String>>> tasks = candidates.stream()
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
