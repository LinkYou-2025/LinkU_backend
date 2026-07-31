package com.umc.linkyou.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job deleteInactiveUserJob;
    private final Job deleteOldAlarmJob;
    private final Job deleteExpiredFcmTokenJob;
    private final Job generateMonthlyCurationJob;
    private final Job sendCurationAlarmJob;

    public BatchScheduler(
            JobLauncher jobLauncher,
            @Qualifier("deleteInactiveUserJob") Job deleteInactiveUserJob,
            @Qualifier("deleteOldAlarmJob") Job deleteOldAlarmJob,
            @Qualifier("deleteExpiredFcmTokenJob") Job deleteExpiredFcmTokenJob,
            @Qualifier("generateMonthlyCurationJob") Job generateMonthlyCurationJob,
            @Qualifier("sendCurationAlarmJob") Job sendCurationAlarmJob
    ) {
        this.jobLauncher = jobLauncher;
        this.deleteInactiveUserJob = deleteInactiveUserJob;
        this.deleteOldAlarmJob = deleteOldAlarmJob;
        this.deleteExpiredFcmTokenJob = deleteExpiredFcmTokenJob;
        this.generateMonthlyCurationJob = generateMonthlyCurationJob;
        this.sendCurationAlarmJob = sendCurationAlarmJob;
    }

    // 매일 새벽 3시 - 비활성 유저 삭제
    @Scheduled(cron = "0 0 3 * * ?")
    public void runDeleteInactiveUserJob() throws Exception {
        log.info("[BATCH] 배치 실행 시작: 비활성 유저 삭제");
        jobLauncher.run(deleteInactiveUserJob, buildJobParameters());
    }

    // 매일 새벽 3시 30분 - 오래된 알림 삭제
    @Scheduled(cron = "0 30 3 * * ?")
    public void runDeleteOldAlarmJob() throws Exception {
        log.info("배치 실행 시작: 오래된 알림 삭제");
        jobLauncher.run(deleteOldAlarmJob, buildJobParameters());
    }

    // 매일 새벽 4시 - 만료된 FCM 토큰 삭제
    @Scheduled(cron = "0 0 4 * * ?")
    public void runDeleteExpiredFcmTokenJob() throws Exception {
        log.info("배치 실행 시작: 만료된 FCM 토큰 삭제");
        jobLauncher.run(deleteExpiredFcmTokenJob, buildJobParameters());
    }

    // 매월 1일 새벽 0시 20분 - 큐레이션 생성
    @Scheduled(cron = "0 20 0 1 * *", zone = "Asia/Seoul")
    public void runGenerateMonthlyCurationJob() throws Exception {
        log.info("배치 실행 시작: 월간 큐레이션 생성");
        jobLauncher.run(generateMonthlyCurationJob, buildJobParameters());
    }

    // 매월 1일 오전 8시 - 큐레이션 알림 발송
    @Scheduled(cron = "0 0 8 1 * *", zone = "Asia/Seoul")
    public void runSendCurationAlarmJob() throws Exception {
        log.info("배치 실행 시작: 큐레이션 알림 발송");
        jobLauncher.run(sendCurationAlarmJob, buildJobParameters());
    }

    private JobParameters buildJobParameters() {
        return new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
    }
}
