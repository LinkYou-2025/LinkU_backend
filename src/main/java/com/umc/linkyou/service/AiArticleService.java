package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.awss3.AwsS3Service;
import com.umc.linkyou.converter.AiArticleConverter;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.AiArticleAnalyzer;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;

import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.web.dto.AiArticleResponseDTO;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiArticleService {

    private final UserRepository userRepository;
    private final LinkuRepository linkuRepository;
    private final AiArticleRepository aiArticleRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final AiArticleAnalyzer aiArticleAnalyzer;
    private final AlarmService alarmService;
    private final AwsS3Service awsS3Service;

    @Transactional
    public AiArticleResponseDTO.AiArticleResultDTO saveAiArticle(Long linkuId, Long userId) {
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        // 동일 (user, linku) 조합으로 저장된 UsersLinku가 여러 건일 수 있다. 응답/알림에는 최신 1건을
        // 대표로 쓰지만, "AI 요약 있음" 표시는 같은 링크를 여러 번 저장한 모든 건에 동일하게 남아야 한다
        // (요약은 linku 단위로 한 번만 생성되므로, 그 link를 저장한 기록이라면 몇 번째 저장이든 동일하게 적용된다).
        List<UsersLinku> usersLinkus = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId);
        UsersLinku usersLinku = usersLinkus.stream()
                .max(Comparator.comparing(UsersLinku::getCreatedAt))
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));

        AiArticleResultDTO result = aiArticleAnalyzer.analyzeByUrl(linku.getLinkuUrl());

        AiArticle article = aiArticleRepository.findByLinku(linku)
                .map(existing -> {
                    existing.updateSummary(result.summary());
                    return existing;
                })
                .orElseGet(() -> aiArticleRepository.save(
                        AiArticleConverter.toEntity(result, linku)
                ));

        usersLinkus.forEach(ul -> ul.markAiExist(true));

        String linkTitle = resolveTitle(linku, usersLinku);

        // 요약 완료 시 링크 요약 알림 발송. 설정 필터링은 sendAlarm 내부에서 처리한다.
        alarmService.sendAlarm(userId, new AlarmRequestDTO.AlarmSendRequestDTO(
                AlarmType.LINK_SUMMARY_COMPLETE, linkuId, new AlarmPayload.LinkTitle(linkTitle)));

        return AiArticleConverter.toDto(article, linku, usersLinku, resolveTags(linku), awsS3Service);
    }

    @Transactional
    public AiArticleResponseDTO.AiArticleResultDTO saveOrGetAiArticle(Long linkuId, Long userId) {
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));

        AiArticle aiArticle = aiArticleRepository.findByLinku(linku).orElse(null);

        if (aiArticle == null || aiArticle.getSummary() == null || aiArticle.getSummary().isBlank()) {
            return saveAiArticle(linkuId, userId);
        } else {
            return showAiArticle(linkuId, userId);
        }
    }

    @Transactional
    public AiArticleResponseDTO.AiArticleResultDTO showAiArticle(Long linkuId, Long userId) {
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        AiArticle article = aiArticleRepository.findByLinku(linku)
                .orElseThrow(() -> new GeneralException(AiArticleErrorStatus._AI_ARTICLE_NOT_FOUND));
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        // 동일 (user, linku) 조합으로 저장된 UsersLinku가 여러 건일 수 있다. 응답에는 최신 1건을 대표로
        // 쓰되, "AI 요약 있음" 표시는 본인이 저장한 모든 건에 동일하게 남긴다 (saveAiArticle과 동일한 패턴).
        // 요청 유저가 이 linku를 저장한 적이 없으면(=UsersLinku 없음) 소유권이 없는 것이므로 예외를 던진다.
        // (다른 유저가 먼저 요약을 만들어둔 linkuId를 알기만 하면 조회되는 것을 막기 위함)
        List<UsersLinku> usersLinkus = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId);
        UsersLinku usersLinku = usersLinkus.stream()
                .max(Comparator.comparing(UsersLinku::getCreatedAt))
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._USER_LINKU_NOT_FOUND));

        // 다른 유저가 먼저 만들어둔 요약이라도, 본인이 직접 조회한 시점부터는 본인 소유 UsersLinku에도
        // "AI 요약 있음"으로 남긴다 (본인이 요청/조회한 적 없는데 true로 보이는 문제를 막기 위해 저장
        // 시점에는 더 이상 자동으로 표시하지 않으므로, 대신 실제 조회 시점에 표시한다).
        usersLinkus.forEach(ul -> ul.markAiExist(true));

        return AiArticleConverter.toDto(article, linku, usersLinku, resolveTags(linku), awsS3Service);
    }

    // AI 요약 호출과 별개로, 링크 저장 시 이미 분류되어 저장된 키워드를 그대로 태그로 사용한다
    // (요약할 때마다 태그를 다시 생성하지 않음 - linku 단위로 한 번 분류된 키워드는 항상 동일해야 함).
    private String resolveTags(Linku linku) {
        return linku.getLinkuKeywords().stream()
                .map(lk -> lk.getKeyword().getName())
                .collect(Collectors.joining(", "));
    }

    // 알림 페이로드에 넣을 제목 계산용. DTO의 title은 AiArticleConverter.toDto가 동일한
    // 우선순위(usersLinku 우선, 없으면 linku)로 자체 계산하므로 여기서는 알림 발송에만 쓰인다.
    private String resolveTitle(Linku linku, UsersLinku usersLinku) {
        return (usersLinku != null && usersLinku.getTitle() != null)
                ? usersLinku.getTitle()
                : linku.getTitle();
    }

    @Transactional(readOnly = true)
    public LinkuResponseDTO.LinkuSliceResultDTO getMyAiArticlesByCategory(Long userId, Long categoryId, Long cursor, int limit) {
        List<UsersLinku> usersLinkus = usersLinkuRepository.fetchAiArticlesByCategoryIdWithCursor(userId, categoryId, cursor, limit);

        boolean hasNext = usersLinkus.size() > limit;
        List<UsersLinku> resultList = hasNext ? usersLinkus.subList(0, limit) : usersLinkus;

        String nextCursor = hasNext
                ? String.valueOf(resultList.get(resultList.size() - 1).getUserLinkuId())
                : null;

        List<LinkuResponseDTO.AiArticleSummaryDTO> linkuResultDTOs = resultList.stream()
                .map(ul -> LinkuConverter.toAiArticleSummaryDTO(ul, awsS3Service))
                .collect(Collectors.toList());

        return LinkuResponseDTO.LinkuSliceResultDTO.builder()
                .linkuList(linkuResultDTOs)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
