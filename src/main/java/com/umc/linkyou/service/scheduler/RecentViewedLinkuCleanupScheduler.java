package com.umc.linkyou.service.scheduler;

import com.umc.linkyou.repository.RecentViewedLinkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecentViewedLinkuCleanupScheduler {

    private final RecentViewedLinkuRepository recentViewedLinkuRepository;

    // 매일 새벽 3시에 자동 실행 (cron 표현식: 초 분 시 일 월 요일)
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupOldRecentViews() {
        // 기준점: 현재 시간으로부터 3개월 전
        LocalDateTime cutoff = LocalDateTime.now().minusMonths(3);
        log.info("[Scheduler] 오래된 열람 로그 정리를 시작합니다. 기준 시간: {}", cutoff);

        int deletedCount = recentViewedLinkuRepository.deleteByViewedAtBefore(cutoff);

        log.info("[Scheduler] 열람 로그 정리 완료. 삭제된 행 개수: {}", deletedCount);
    }
}