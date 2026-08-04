package com.umc.linkyou.batch.curation;

import com.umc.linkyou.domain.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 매월 1일마다 유저별 큐레이션을 생성하는 배치 작업 설정
 * 생성된 큐레이션에 대한 알림 발송은 발송 시각이 달라(오전 8시) {@link SendCurationAlarmJobConfig}의 별도 Job으로 분리되어 있음
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class GenerateMonthlyCurationJobConfig {

    private static final int CHUNK_SIZE = 100;
    // 유저 개별 처리 실패가 전체 배치를 막지 않도록 스킵 허용치를 둔다.
    // 이 값을 넘기면 데이터/설정 문제로 보고 Step을 FAILED 처리해 배치를 중단시킨다.
    private static final int SKIP_LIMIT = 50;

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
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(SKIP_LIMIT)
                .listener(skipListener())
                .build();
    }

    private SkipListener<Users, MonthlyCurationBatchItem> skipListener() {
        return new SkipListener<>() {
            @Override
            public void onSkipInProcess(Users item, Throwable t) {
                log.warn("[CurationBatch] 유저 처리 스킵 userId={}, cause={}", item.getId(), t.getMessage());
            }

            @Override
            public void onSkipInWrite(MonthlyCurationBatchItem item, Throwable t) {
                log.warn("[CurationBatch] 큐레이션 생성 스킵 userId={}, cause={}",
                        item.user().getId(), t.getMessage());
            }
        };
    }
}
