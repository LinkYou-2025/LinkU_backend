package com.umc.linkyou.service.curation.recommend.external;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.service.common.EmotionMapper;
import com.umc.linkyou.service.common.ImageFetchService;
import com.umc.linkyou.service.common.UrlReachabilityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalRecommendWorker {

    private static final int EXTERNAL_LIMIT = 5;
    // 접속할 수 없는 링크가 걸러지므로 필요한 개수보다 여유 있게 요청한다.
    private static final int EXTERNAL_SEARCH_LIMIT = 8;

    private final CurationRepository curationRepository;
    private final CurationLinkuRepository curationLinkuRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final AiSearchService aiSearchService;
    private final ImageFetchService imageFetchService;
    private final EmotionMapper emotionMapper;
    private final SituationRepository situationRepository;
    private final UrlReachabilityService urlReachabilityService;

    @Transactional
    public void generateExternal(Long curationId) {
        log.info("[EXT] 외부 추천 시작 curationId={}", curationId);

        Curation curation =
                curationRepository
                        .findById(curationId)
                        .orElseThrow(
                                () ->
                                        new GeneralException(
                                                CurationErrorStatus._CURATION_NOT_FOUND));
        Users user = curation.getUser();
        Long userId = user.getId();

        // curation.baseMonth는 이슈 월(N). 데이터 집계 기간은 N-1월이다.
        String dataBaseMonth = YearMonth.parse(curation.getBaseMonth()).minusMonths(1).toString();

        List<String> topTags =
                keywordMonthlyCountRepository
                        .findTopByUserIdAndBaseMonth(userId, dataBaseMonth, PageRequest.of(0, 3))
                        .stream()
                        .map(
                                kmc ->
                                        kmc.getType() == KeywordType.EMOTION
                                                ? emotionMapper.getEmotionName(kmc.getRefId())
                                                : situationRepository
                                                        .findNameById(kmc.getRefId())
                                                        .orElse(""))
                        .filter(name -> !name.isBlank())
                        .toList();

        String jobName = user.getJob() != null ? user.getJob().getName() : null;
        String gender = user.getGender() != null ? user.getGender().name() : null;

        List<ExternalLinkResultDTO> external;
        try {
            long t0 = System.currentTimeMillis();
            external =
                    aiSearchService.searchExternalLinks(
                            new ExternalSearchRequest(
                                    topTags, EXTERNAL_SEARCH_LIMIT, jobName, gender));
            log.info("[AI] 외부 추천 응답 elapsed={}ms", System.currentTimeMillis() - t0);
        } catch (Exception e) {
            log.warn("[AI] 외부 추천 실패, 기존 데이터 유지 curationId={}", curationId, e);
            return;
        }

        if (external.isEmpty()) {
            log.warn("[EXT] AI 응답 없음, 기존 데이터 유지 curationId={}", curationId);
            return;
        }

        List<Map.Entry<ExternalLinkResultDTO, CompletableFuture<Optional<String>>>> reachChecks =
                external.stream()
                        .filter(item -> item.getUrl() != null && !item.getUrl().isBlank())
                        .map(
                                item ->
                                        Map.entry(
                                                item,
                                                urlReachabilityService.resolveAsync(item.getUrl())))
                        .toList();
        // 서로 다른 주소가 같은 URL로 정리되면 먼저 나온 것만 남긴다.
        Map<String, ResolvedLink> resolvedByUrl = new LinkedHashMap<>();
        reachChecks.forEach(
                entry ->
                        entry.getValue()
                                .join()
                                .ifPresent(
                                        url ->
                                                resolvedByUrl.putIfAbsent(
                                                        url,
                                                        new ResolvedLink(
                                                                url, entry.getKey().getTitle()))));
        List<ResolvedLink> reachable = List.copyOf(resolvedByUrl.values());

        if (reachable.isEmpty()) {
            log.warn("[EXT] 도달 가능한 링크 없음, 기존 데이터 유지 curationId={}", curationId);
            return;
        }

        // 이미 저장한 링크는 빼고 EXTERNAL_LIMIT개까지 채운다.
        // 링크는 대표 URL로 저장되므로, 리다이렉트를 정리한 URL로 비교한다.
        List<String> normalizedUrls = reachable.stream().map(ResolvedLink::url).toList();
        List<UsersLinku> savedMatches =
                usersLinkuRepository.findByUserIdAndLinkuUrlIn(userId, normalizedUrls);
        Set<String> savedUrls =
                savedMatches.stream()
                        .map(ul -> ul.getLinku().getLinkuUrl())
                        .collect(Collectors.toSet());

        List<ResolvedLink> candidates =
                reachable.stream()
                        .filter(item -> !savedUrls.contains(item.url()))
                        .limit(EXTERNAL_LIMIT)
                        .toList();

        if (candidates.isEmpty()) {
            log.warn("[EXT] 저장되지 않은 도달 가능 링크 없음, 기존 데이터 유지 curationId={}", curationId);
            return;
        }

        List<Map.Entry<ResolvedLink, CompletableFuture<String>>> tasks =
                candidates.stream()
                        .map(
                                item ->
                                        Map.entry(
                                                item,
                                                imageFetchService.fetchAsync(
                                                        item.url(), item.title())))
                        .toList();

        curationLinkuRepository.deleteAllByCurationIdAndType(
                curationId, CurationLinkuType.EXTERNAL);

        List<CurationLinku> toSave =
                tasks.stream()
                        .map(
                                entry ->
                                        CurationLinku.ofExternal(
                                                curation,
                                                entry.getKey().url(),
                                                entry.getKey().title(),
                                                entry.getValue().join()))
                        .toList();
        curationLinkuRepository.saveAll(toSave);

        log.info("[EXT] 외부 추천 저장 완료 curationId={}, rows={}", curationId, toSave.size());
    }

    private record ResolvedLink(String url, String title) {}
}
