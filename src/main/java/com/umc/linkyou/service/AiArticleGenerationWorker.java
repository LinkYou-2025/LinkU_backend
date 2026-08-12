package com.umc.linkyou.service;

import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.AiArticleAnalyzer;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * AiArticleService.createAiArticle()이 커밋해둔 PENDING 레코드를 실제로 채우는 비동기 워커.
 *
 * AiArticleService 안에 두지 않고 별도 빈으로 분리한 이유: {@code @Async}는 스프링 프록시를 거쳐야
 * 동작하는데, 같은 클래스 안에서 this.generateAsync(...)로 자기 자신을 호출하면 프록시를 우회해서
 * 실제로는 동기 실행되어 버린다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiArticleGenerationWorker {

    private final AiArticleRepository aiArticleRepository;
    private final UsersLinkuRepository usersLinkuRepository;
    private final AiArticleAnalyzer aiArticleAnalyzer;
    private final AlarmService alarmService;

    // userId: 생성을 요청한 사용자. 크롤링/Gemini 응답을 기다리지 않고 즉시 리턴하므로
    // 호출부(AiArticleService.createAiArticle)는 이 메서드의 완료를 기다리지 않는다.
    // aiExist 표시와 완료 알림은 "요청한 본인"에게만 남긴다 (리팩터링 전 AiArticleService의 기존 정책과 동일).
    @Async("aiArticleTaskExecutor")
    @Transactional
    public void generateAsync(Long aiArticleId, Long userId) {
        AiArticle article = aiArticleRepository.findById(aiArticleId).orElse(null);
        if (article == null) {
            log.warn("[AI 요약 생성] AiArticle(id={})을 찾을 수 없어 생성을 건너뜁니다.", aiArticleId);
            return;
        }

        Linku linku = article.getLinku();
        Long linkuId = linku.getLinkuId();

        try {
            AiArticleResultDTO result = aiArticleAnalyzer.analyzeByUrl(linku.getLinkuUrl());
            article.complete(result.summary());

            List<UsersLinku> usersLinkus = usersLinkuRepository.findByUser_IdAndLinku_LinkuId(userId, linkuId);
            usersLinkus.forEach(ul -> ul.markAiExist(true));

            UsersLinku latest = usersLinkus.stream()
                    .max(Comparator.comparing(UsersLinku::getCreatedAt))
                    .orElse(null);
            String linkTitle = resolveTitle(linku, latest);

            alarmService.sendAlarm(userId, new AlarmRequestDTO.AlarmSendRequestDTO(
                    AlarmType.LINK_SUMMARY_COMPLETE, linkuId, new AlarmPayload.LinkTitle(linkTitle)));
        } catch (GeneralException e) {
            String reasonCode = e.getCode().getReason().getCode();
            log.warn("[AI 요약 생성 실패] linkuId={}, code={}, message={}", linkuId, reasonCode, e.getMessage());
            article.markFailed(reasonCode);
        } catch (Exception e) {
            log.warn("[AI 요약 생성 실패] linkuId={}, 알 수 없는 오류: {}", linkuId, e.getMessage(), e);
            article.markFailed("UNKNOWN");
        }
    }

    private String resolveTitle(Linku linku, UsersLinku usersLinku) {
        return (usersLinku != null && usersLinku.getTitle() != null)
                ? usersLinku.getTitle()
                : linku.getTitle();
    }
}
