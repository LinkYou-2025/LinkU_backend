package com.umc.linkyou.batch.curation;

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
 * 지난달 생성된 큐레이션에 대한 알림을 발송하는 배치 작업 설정
 *
 * <p>큐레이션 생성(generateMonthlyCurationJob)과는 발송 시각이 달라(매월 1일 오전 8시) 별도 Job으로 분리했다.
 * 발송 대상은 Alarm 존재 여부로 판단하므로, 이 Job만 재실행돼도 이미 보낸 유저는 다시 대상이 되지 않는다.
 */
@Configuration
@RequiredArgsConstructor
public class SendCurationAlarmJobConfig {

    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final CurationAlarmItemReader curationAlarmItemReader;
    private final CurationAlarmItemProcessor curationAlarmItemProcessor;
    private final CurationAlarmItemWriter curationAlarmItemWriter;

    @Bean
    public Job sendCurationAlarmJob() {
        return new JobBuilder("sendCurationAlarmJob", jobRepository)
                .start(sendCurationAlarmStep())
                .build();
    }

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
