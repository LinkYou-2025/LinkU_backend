package com.umc.linkyou.service.curation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurationBatchScheduler {
    private final CurationService curationService;

    // 한국 표준시 기준 매달 1일 새벽 0시 5분 실행
    @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Seoul")
    public void runMonthlyCurationBatch() {
        log.info("[CurationBatch] 월간 큐레이션 자동 생성 시작");
        curationService.generateMonthlyCurationForAllUsers();
        log.info("[CurationBatch] 월간 큐레이션 자동 생성 완료");
    }
}
