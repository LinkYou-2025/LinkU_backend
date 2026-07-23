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
 * 매월 1일마다 유저별 큐레이션을 생성하고, 이어서 생성된 큐레이션에 대한 알림을 발송하는 배치 작업 설정
 * 두 Step 모두 청크 기반으로 구현
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
    private final CurationAlarmItemReader curationAlarmItemReader;
    private final CurationAlarmItemProcessor curationAlarmItemProcessor;
    private final CurationAlarmItemWriter curationAlarmItemWriter;

    @Bean
    public Job generateMonthlyCurationJob() {
        return new JobBuilder("generateMonthlyCurationJob", jobRepository)
                .start(generateMonthlyCurationStep())
                .next(sendCurationAlarmStep())
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

    // 큐레이션 생성 실패와 알림 발송 실패를 분리하기 위해 별도 Step으로 구성한다.
    // 발송 대상은 Alarm 존재 여부로 판단하므로, 이 Step만 재실행돼도 이미 보낸 유저는 다시 대상이 되지 않는다.
    @Bean
    public Step sendCurationAlarmStep() {
        return new StepBuilder("sendCurationAlarmStep", jobRepository)
                .<CurationAlarmCandidate, CurationAlarmBatchItem>chunk(CHUNK_SIZE, transactionManager)
                .reader(curationAlarmItemReader)
                .processor(curationAlarmItemProcessor)
                .writer(curationAlarmItemWriter)
                .build();
    }
}
