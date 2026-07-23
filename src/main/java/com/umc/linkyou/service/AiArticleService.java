package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.AiArticleConverter;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Domain;
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

        // 요약 완료 시 링크 요약 알림 발송. 설정 필터링은 sendAlarm 내부에서 처리한다.
        String linkTitle = usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle();
        alarmService.sendAlarm(userId, new AlarmRequestDTO.AlarmSendRequestDTO(
                AlarmType.LINK_SUMMARY_COMPLETE, linkuId, new AlarmPayload.LinkTitle(linkTitle)));

        return AiArticleConverter.toDto(article, linku, usersLinku);
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

    public AiArticleResponseDTO.AiArticleResultDTO showAiArticle(Long linkuId, Long userId) {
        Linku linku = linkuRepository.findById(linkuId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        AiArticle article = aiArticleRepository.findByLinku(linku)
                .orElseThrow(() -> new GeneralException(AiArticleErrorStatus._AI_ARTICLE_NOT_FOUND));
        userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        // 동일 (user, linku) 조합으로 저장된 UsersLinku가 여러 건일 수 있어 최신 1건을 사용한다.
        UsersLinku usersLinku = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId).stream()
                .max(Comparator.comparing(UsersLinku::getCreatedAt))
                .orElse(null);

        return AiArticleConverter.toDto(article, linku, usersLinku);
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
                .map(ul -> {
                    Linku l = ul.getLinku();
                    Domain domain = l.getDomain();
                    return LinkuResponseDTO.AiArticleSummaryDTO.builder()
                            .linkuId(l.getLinkuId())
                            .linku(l.getLinkuUrl())
                            .emotionId(ul.getEmotion() != null ? ul.getEmotion().getEmotionId() : null)
                            .domain(domain != null ? domain.getName() : null)
                            .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                            .title(ul.getTitle() != null ? ul.getTitle() : l.getTitle())
                            .linkuImageUrl(ul.getImageUrl() != null ? ul.getImageUrl() : l.getImgUrl())
                            .build();
                })
                .collect(Collectors.toList());

        return LinkuResponseDTO.LinkuSliceResultDTO.builder()
                .linkuList(linkuResultDTOs)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }
}
