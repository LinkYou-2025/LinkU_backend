package com.umc.linkyou.batch.curation;

import com.umc.linkyou.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 매월 1일마다 유저별 큐레이션을 생성하는 배치 작업 설정
 * 청크 기반으로 구현
 */
@Configuration
@RequiredArgsConstructor
public class GenerateMonthlyCurationJobConfig {

    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CurationItemReader curationItemReader;
    private final CurationItemProcessor curationItemProcessor;
    private final CurationItemWriter curationItemWriter;

    @Bean
    public Job generateMonthlyCurationJob() {
        return new JobBuilder("generateMonthlyCurationJob", jobRepository)
                .start(generateMonthlyCurationStep())
                .build();
    }

    @Bean
    public Step generateMonthlyCurationStep() {
        return new StepBuilder("generateMonthlyCurationStep", jobRepository)
                .<Users, MonthlyCurationBatchItem>chunk(CHUNK_SIZE, transactionManager)
                .reader(curationItemReader)
                .processor(curationItemProcessor)
                .writer(curationItemWriter)
                .build();
    }
}
