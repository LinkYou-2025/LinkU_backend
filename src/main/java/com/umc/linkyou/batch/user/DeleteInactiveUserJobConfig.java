package com.umc.linkyou.batch.user;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.umc.linkyou.domain.Users;

/**
 * 비활성 사용자 정리 배치 작업
 *
 * <p>유저 정리는 단순 일괄 삭제보다 사용자별 상태 판단과 후처리 가능성이 커서
 * chunk 기반 step으로 분리
 */
@Configuration
@RequiredArgsConstructor
public class DeleteInactiveUserJobConfig {

    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final InactiveUserItemReader inactiveUserItemReader;
    private final InactiveUserItemProcessor inactiveUserItemProcessor;
    private final InactiveUserItemWriter inactiveUserItemWriter;

    @Bean
    public Job deleteInactiveUserJob() {
        return new JobBuilder("deleteInactiveUserJob", jobRepository)
                .start(deleteInactiveUserStep())
                .build();
    }

    @Bean
    public Step deleteInactiveUserStep() {
        return new StepBuilder("deleteInactiveUserStep", jobRepository)
                .<Users, Users>chunk(CHUNK_SIZE, transactionManager)
                .reader(inactiveUserItemReader)
                .processor(inactiveUserItemProcessor)
                .writer(inactiveUserItemWriter)
                .build();
    }
}
