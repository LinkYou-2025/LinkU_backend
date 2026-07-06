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
import com.umc.linkyou.domain.Users;
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
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        UsersLinku usersLinku = usersLinkuRepository.findByUserAndLinku(user, linku)
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

        usersLinku.markAiExist(true);

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
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        UsersLinku usersLinku = usersLinkuRepository.findByUserAndLinku(user, linku)
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

        List<LinkuResponseDTO.LinkuResultDTO> linkuResultDTOs = resultList.stream()
                .map(ul -> {
                    Linku l = ul.getLinku();
                    AiArticle a = l.getAiArticle();
                    String keyword = l.getLinkuKeywords().stream()
                            .map(lk -> lk.getKeyword().getName())
                            .collect(Collectors.joining(", "));
                    return LinkuResponseDTO.LinkuResultDTO.builder()
                            .userId(userId)
                            .userLinkuId(ul.getUserLinkuId())
                            .linkuId(l.getLinkuId())
                            .categoryId(l.getCategory().getCategoryId())
                            .linku(l.getLinkuUrl())
                            .memo(ul.getMemo())
                            .emotionId(ul.getEmotion().getEmotionId())
                            .title(ul.getTitle() != null ? ul.getTitle() : l.getTitle())
                            .summary(a != null ? a.getSummary() : "요약 정보가 없습니다.")
                            .keyword(keyword)
                            .linkuImageUrl(ul.getImageUrl() != null ? ul.getImageUrl() : l.getImgUrl())
                            .aiArticleExists(ul.getAiExist())
                            .createdAt(ul.getCreatedAt())
                            .updatedAt(ul.getUpdatedAt())
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
