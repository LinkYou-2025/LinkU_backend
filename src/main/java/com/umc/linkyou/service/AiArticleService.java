package com.umc.linkyou.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.AiArticleConverter;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.enums.PageType;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.AiArticleAnalyzer;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.web.dto.AiArticleResponseDTO;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiArticleService {

    private final UserRepository userRepository;
    private final AiArticleRepository aiArticleRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final AiArticleAnalyzer aiArticleAnalyzer;
    private final AlarmService alarmService;
    private final TransactionTemplate transactionTemplate;

    private static final int PAGE_TYPE_RECHECK_DAYS = 7;

    // 크롤링과 AI 호출은 커넥션 풀 고갈을 방지하기 위해 트랜잭션 밖에서 한다.
    public AiArticleResponseDTO.AiArticleResultDTO saveAiArticle(Long userLinkuId, Long userId) {
        SummaryTarget target =
                transactionTemplate.execute(status -> loadSummarizableUrl(userLinkuId, userId));

        // 요약 결과의 pageType으로 재검증해 따로 요청하지 않는다.
        AiArticleResultDTO result = aiArticleAnalyzer.analyzeByUrl(target.url());

        // AI 오판으로 콘텐츠 링크가 막히지 않게 일반 요청에서는 요약 결과의 pageType을 무시한다.
        if (target.requiresPageTypeRecheck()) {
            PageType rechecked = result.resolvedPageType();
            transactionTemplate.execute(
                    status -> {
                        recordRecheckedPageType(userLinkuId, userId, rechecked);
                        return null;
                    });
            if (!isSummarizable(rechecked)) {
                throw new GeneralException(AiArticleErrorStatus._NOT_SUMMARIZABLE_PAGE);
            }
        }

        return transactionTemplate.execute(status -> persistSummary(userLinkuId, userId, result));
    }

    private SummaryTarget loadSummarizableUrl(Long userLinkuId, Long userId) {
        userRepository
                .findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        Linku linku = getOwnedUsersLinku(userId, userLinkuId).getLinku();
        // 비콘텐츠 링크는 요약하지 않는다. 판별 후 7일이 지났으면 재검증한다.
        if (!linku.isSummarizable()
                && !linku.needsPageTypeRecheck(
                        LocalDateTime.now().minusDays(PAGE_TYPE_RECHECK_DAYS))) {
            throw new GeneralException(AiArticleErrorStatus._NOT_SUMMARIZABLE_PAGE);
        }
        return new SummaryTarget(linku.getLinkuUrl(), !linku.isSummarizable());
    }

    // 판별 실패(UNKNOWN)는 null로 남겨 요약을 허용하고, 확인 시각은 기록해 재검증 주기를 다시 시작한다.
    private void recordRecheckedPageType(Long userLinkuId, Long userId, PageType rechecked) {
        Linku linku = getOwnedUsersLinku(userId, userLinkuId).getLinku();
        linku.updatePageType(rechecked == PageType.UNKNOWN ? null : rechecked, LocalDateTime.now());
    }

    private static boolean isSummarizable(PageType pageType) {
        return pageType == PageType.CONTENT || pageType == PageType.UNKNOWN;
    }

    private record SummaryTarget(String url, boolean requiresPageTypeRecheck) {}

    private AiArticleResponseDTO.AiArticleResultDTO persistSummary(
            Long userLinkuId, Long userId, AiArticleResultDTO result) {
        // 외부 호출 사이에 엔티티가 바뀌었을 수 있어 다시 조회한다.
        UsersLinku usersLinku = getOwnedUsersLinku(userId, userLinkuId);
        Linku linku = usersLinku.getLinku();
        // 요약은 링크 단위라, 같은 링크를 여러 번 저장했다면 모든 저장 건에 요약 있음을 표시한다.
        List<UsersLinku> usersLinkus =
                usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linku.getLinkuId());

        AiArticle article =
                aiArticleRepository
                        .findByLinku(linku)
                        .map(
                                existing -> {
                                    existing.updateSummary(result.summary());
                                    return existing;
                                })
                        .orElseGet(
                                () ->
                                        aiArticleRepository.save(
                                                AiArticleConverter.toEntity(result, linku)));

        usersLinkus.forEach(ul -> ul.markAiExist(true));

        String linkTitle = resolveTitle(linku, usersLinku);

        // 알림을 누르면 요청한 저장 링크로 이동하도록 userLinkuId를 넘긴다.
        alarmService.sendAlarm(
                userId,
                new AlarmRequestDTO.AlarmSendRequestDTO(
                        AlarmType.LINK_SUMMARY_COMPLETE,
                        usersLinku.getUserLinkuId(),
                        new AlarmPayload.LinkTitle(linkTitle)));

        return AiArticleConverter.toDto(article, linku, usersLinku, resolveTags(linku));
    }

    // 생성 경로에 외부 호출이 있어 메서드 전체에 트랜잭션을 걸지 않는다.
    public AiArticleResponseDTO.AiArticleResultDTO saveOrGetAiArticle(
            Long userLinkuId, Long userId) {
        boolean hasSummary =
                Boolean.TRUE.equals(
                        transactionTemplate.execute(
                                status -> {
                                    Linku linku =
                                            getOwnedUsersLinku(userId, userLinkuId).getLinku();
                                    return aiArticleRepository
                                            .findByLinku(linku)
                                            .map(
                                                    article ->
                                                            article.getSummary() != null
                                                                    && !article.getSummary()
                                                                            .isBlank())
                                            .orElse(false);
                                }));

        if (hasSummary) {
            return transactionTemplate.execute(status -> showAiArticle(userLinkuId, userId));
        }
        return saveAiArticle(userLinkuId, userId);
    }

    @Transactional
    public AiArticleResponseDTO.AiArticleResultDTO showAiArticle(Long userLinkuId, Long userId) {
        UsersLinku usersLinku = getOwnedUsersLinku(userId, userLinkuId);
        Linku linku = usersLinku.getLinku();
        AiArticle article =
                aiArticleRepository
                        .findByLinku(linku)
                        .orElseThrow(
                                () ->
                                        new GeneralException(
                                                AiArticleErrorStatus._AI_ARTICLE_NOT_FOUND));
        userRepository
                .findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        List<UsersLinku> usersLinkus =
                usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linku.getLinkuId());

        // 다른 유저가 만든 요약이라도 본인이 조회한 시점부터 본인 저장 건에 요약 있음을 표시한다.
        usersLinkus.forEach(ul -> ul.markAiExist(true));

        return AiArticleConverter.toDto(article, linku, usersLinku, resolveTags(linku));
    }

    private UsersLinku getOwnedUsersLinku(Long userId, Long userLinkuId) {
        UsersLinku usersLinku =
                usersLinkuRepository
                        .findById(userLinkuId)
                        .orElseThrow(
                                () -> new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));
        if (!usersLinku.getUser().getId().equals(userId)) {
            throw new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND);
        }
        return usersLinku;
    }

    // 태그는 요약할 때 다시 만들지 않고 링크 저장 때 분류한 키워드를 쓴다.
    private String resolveTags(Linku linku) {
        return linku.getLinkuKeywords().stream()
                .map(lk -> lk.getKeyword().getName())
                .collect(Collectors.joining(", "));
    }

    // 알림 제목용
    private String resolveTitle(Linku linku, UsersLinku usersLinku) {
        return (usersLinku != null && usersLinku.getTitle() != null)
                ? usersLinku.getTitle()
                : linku.getTitle();
    }

    // categoryId가 null이면 전체 카테고리를 조회한다.
    @Transactional(readOnly = true)
    public LinkuResponseDTO.LinkuSliceResultDTO getMyAiArticlesByCategory(
            Long userId, Long categoryId, Long cursor, int limit) {
        List<UsersLinku> usersLinkus =
                usersLinkuRepository.fetchAiArticlesByCategoryIdWithCursor(
                        userId, categoryId, cursor, limit);

        boolean hasNext = usersLinkus.size() > limit;
        List<UsersLinku> resultList = hasNext ? usersLinkus.subList(0, limit) : usersLinkus;

        Long nextCursor = hasNext ? resultList.get(resultList.size() - 1).getUserLinkuId() : null;

        List<LinkuResponseDTO.AiArticleSummaryDTO> linkuResultDTOs =
                resultList.stream()
                        .map(LinkuConverter::toAiArticleSummaryDTO)
                        .collect(Collectors.toList());

        return LinkuResponseDTO.LinkuSliceResultDTO.builder()
                .linkuList(linkuResultDTOs)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
