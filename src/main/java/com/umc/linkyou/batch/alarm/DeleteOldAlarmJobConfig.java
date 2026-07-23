package com.umc.linkyou.batch.alarm;

import com.umc.linkyou.repository.AlarmRepository;
import com.umc.linkyou.repository.UserAlarmRepository;
import com.umc.linkyou.service.alarm.AlarmService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;

/**
 * 30일 이상된 알람과 사용자 알람을 삭제하는 배치 작업 설정
 * - Tasklet 기반으로 구현하여 간단한 삭제 작업 수행
 */
@Configuration
@RequiredArgsConstructor
public class DeleteOldAlarmJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final AlarmRepository alarmRepository;
    private final UserAlarmRepository userAlarmRepository;

    // 오래된 알림 삭제 잡
    @Bean
    public Job deleteOldAlarmJob() {
        return new JobBuilder("deleteOldAlarmJob", jobRepository)
                .start(deleteOldAlarmStep())
                .build();
    }

    // 오래된 알림 삭제 스텝
    @Bean
    public Step deleteOldAlarmStep() {
        return new StepBuilder("deleteOldAlarmStep", jobRepository)
                .tasklet(deleteOldAlarmTasklet(), transactionManager)
                .build();
    }

    // Tasklet 기반 간단한 삭제 작업 수행
    @Bean
    public Tasklet deleteOldAlarmTasklet() {
        return (contribution, chunkContext) -> {
            // 30일 이전의 알람과 사용자 알림 삭제
            LocalDateTime threshold = LocalDateTime.now().minusDays(30);
            userAlarmRepository.deleteByCreatedAtBefore(threshold);
            alarmRepository.deleteByCreatedAtBefore(threshold);
            return RepeatStatus.FINISHED;
        };
    }
}
