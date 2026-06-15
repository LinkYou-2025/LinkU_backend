package com.umc.linkyou.batch.alarm;

import com.umc.linkyou.repository.UserFcmTokenRepository;
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
 * FCM 토큰 중 270일 간 사용하지 않는 토큰을 즉시 삭제하는 배치 작업 설정
 * - Tasklet 기반으로 구현하여 간단한 삭제 작업 수행
 */
@Configuration
@RequiredArgsConstructor
public class DeleteExpiredFcmTokenJobConfig {

    private final JobRepository jobRepository;
    private final UserFcmTokenRepository userFcmTokenRepository;
    private final PlatformTransactionManager transactionManager;

    @Bean
    public Job deleteExpiredFcmTokenJob() {
        return new JobBuilder("deleteExpiredFcmTokenJob", jobRepository)
                .start(deleteExpiredFcmTokenStep())
                .build();
    }

    @Bean
    public Step deleteExpiredFcmTokenStep() {
        return new StepBuilder("deleteExpiredFcmTokenStep", jobRepository)
                .tasklet(deleteExpiredFcmTokenTasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet deleteExpiredFcmTokenTasklet() {
        return (contribution, chunkContext) -> {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(270);
            userFcmTokenRepository.deleteByLastUsedAtBefore(cutoff);
            return RepeatStatus.FINISHED;
        };
    }
}
