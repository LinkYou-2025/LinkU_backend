package com.umc.linkyou.service.scheduler;

import com.umc.linkyou.service.curation.CurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurationBatchScheduler {

    private final CurationService curationService;

    @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Seoul")
    public void runMonthlyCurationBatch() {
        System.out.println("✅ 자동 큐레이션 생성 실행");
        curationService.generateMonthlyCurationForAllUsers();
    }
}

//테스트용
//@Component
//@RequiredArgsConstructor
//public class CurationBatchScheduler {
//
//    private final CurationService curationService;
//
//    @PostConstruct
//    public void scheduleOnceAfterFiveMinutes() {
//        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
//
//        scheduler.schedule(() -> {
//            System.out.println("✅ 1분 후 자동 큐레이션 생성 실행");
//            curationService.generateMonthlyCurationForAllUsers();
//        }, 1, TimeUnit.MINUTES);
//    }
//}