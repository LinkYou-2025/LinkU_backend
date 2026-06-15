package com.umc.linkyou.batch;

import com.umc.linkyou.batch.curation.MonthlyCurationBatchItem;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.curation.CurationBatchService;
import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:curation-batch-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.enabled=false",
        "spring.sql.init.mode=never"
})
@DisplayName("GenerateMonthlyCurationJob 통합 테스트")
class GenerateMonthlyCurationBatchIntegrationTest {

    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobRepository jobRepository;
    @Autowired @Qualifier("generateMonthlyCurationJob") private Job generateMonthlyCurationJob;

    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurationBatchService curationBatchService;

    private JobLauncherTestUtils jobLauncherTestUtils;
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(generateMonthlyCurationJob);

        jobRepositoryTestUtils = new JobRepositoryTestUtils(jobRepository);
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM users");
    }

    // ── 정상 처리 ────────────────────────────────────────────

    @Nested
    @DisplayName("정상 처리")
    class 정상처리 {

        @Test
        @DisplayName("전체 사용자 수만큼 Processor가 호출되고 Writer까지 전달된다")
        void 전체_사용자_Read_Write_완료() throws Exception {
            // given
            saveUser("user-a");
            saveUser("user-b");
            saveUser("user-c");

            given(curationBatchService.toBatchItem(any(Users.class)))
                    .willReturn(Optional.of(mockBatchItem()));

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            StepExecution step = execution.getStepExecutions().iterator().next();
            assertThat(step.getReadCount()).isEqualTo(3);
            assertThat(step.getWriteCount()).isEqualTo(3);

            verify(curationBatchService, atLeastOnce()).createMonthlyCurations(any(List.class));
        }

        @Test
        @DisplayName("Processor가 empty를 반환한 사용자는 Writer에 전달되지 않는다")
        void Processor_empty_반환_사용자_Writer_스킵() throws Exception {
            // given
            saveUser("eligible-user");
            saveUser("skip-user");

            given(curationBatchService.toBatchItem(any(Users.class)))
                    .willReturn(Optional.of(mockBatchItem()))
                    .willReturn(Optional.empty());

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            StepExecution step = execution.getStepExecutions().iterator().next();
            assertThat(step.getReadCount()).isEqualTo(2);
            assertThat(step.getWriteCount()).isEqualTo(1);
        }
    }


    @Nested
    @DisplayName("엣지 케이스")
    class 엣지케이스 {

        @Test
        @DisplayName("사용자가 없으면 Writer가 호출되지 않고 잡이 정상 완료된다")
        void 사용자_없음_Writer_미호출_COMPLETED() throws Exception {
            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            StepExecution step = execution.getStepExecutions().iterator().next();
            assertThat(step.getReadCount()).isEqualTo(0);
            assertThat(step.getWriteCount()).isEqualTo(0);

            verify(curationBatchService, never()).createMonthlyCurations(any());
        }
    }


    private void saveUser(String nickName) {
        userRepository.save(Users.builder().nickName(nickName).password("pw").build());
    }

    private MonthlyCurationBatchItem mockBatchItem() {
        return new MonthlyCurationBatchItem(null, "2025-05", "http://thumbnail.url");
    }

    private JobParameters uniqueJobParameters() {
        return new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
    }
}
