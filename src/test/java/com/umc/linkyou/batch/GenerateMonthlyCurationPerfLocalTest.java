package com.umc.linkyou.batch;

import com.umc.linkyou.domain.AlarmSetting;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.alarm.FcmServiceImpl;
import com.umc.linkyou.support.config.TestExternalConfig;
import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 월간 큐레이션 배치 로컬 성능/누락 확인용 수동 테스트
 *
 * <p>실제 CurationBatchService/CurationAlarmBatchService를 그대로 태워 유저 N명을
 * 시드하고, 큐레이션 생성 Job과 알림 발송 Job을 순서대로 실행한 뒤(두 Job은 발송 시각이 달라
 * 운영에서도 별도로 스케줄링됨), Step별 read/write/skip count와 소요시간을 출력함.
 * FCM 실전송만 mock 처리함
 *
 * <p>CI에 매번 끼면 안 되므로 기본은 @Disabled. 로컬에서 확인할 때만 아래 애노테이션을
 * 주석 처리하고 실행한 뒤 다시 원복함
 */
@Disabled("로컬 성능 확인용 수동 테스트 — 실행 시 이 줄만 주석 처리 후 실행하고 끝나면 원복할 것")
@SpringBootTest
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
@TestPropertySource(properties = {
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.enabled=false"
})
@DisplayName("월간 큐레이션 배치 로컬 성능 확인")
class GenerateMonthlyCurationPerfLocalTest {

    // 필요에 따라 조정해서 실행
    private static final int USER_COUNT = 500;

    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobRepository jobRepository;
    @Autowired @Qualifier("generateMonthlyCurationJob") private Job generateMonthlyCurationJob;
    @Autowired @Qualifier("sendCurationAlarmJob") private Job sendCurationAlarmJob;

    @Autowired private UserRepository userRepository;
    @Autowired private AlarmSettingRepository alarmSettingRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    // FCM 실전송만 막고, 큐레이션 생성/알림 저장 로직은 전부 실제로 태움
    @MockitoBean private FcmServiceImpl fcmServiceImpl;

    private JobLauncherTestUtils jobLauncherTestUtils;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);

        new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM user_alarms");
        jdbcTemplate.execute("DELETE FROM alarms");
        jdbcTemplate.execute("DELETE FROM curations");
        jdbcTemplate.execute("DELETE FROM alarm_settings");
        jdbcTemplate.execute("DELETE FROM users");
    }

    @Test
    @DisplayName("유저 N명 시드 후 두 Job을 순서대로 실행하고 소요시간/read·write·skip count를 출력한다")
    void 전체_잡_실행_시간_측정() throws Exception {
        // given
        seedUsers(USER_COUNT);

        // when - 운영에서도 자정에 생성, 오전 8시에 발송으로 별도 스케줄링되므로 순서대로 실행
        long start = System.currentTimeMillis();

        jobLauncherTestUtils.setJob(generateMonthlyCurationJob);
        JobExecution curationExecution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

        jobLauncherTestUtils.setJob(sendCurationAlarmJob);
        JobExecution alarmExecution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

        long elapsedMs = System.currentTimeMillis() - start;

        // then
        assertThat(curationExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(alarmExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        System.out.printf("%n[PERF] 유저 %d명, 두 Job 합산 소요시간: %dms%n", USER_COUNT, elapsedMs);
        for (StepExecution step : Stream.concat(
                curationExecution.getStepExecutions().stream(),
                alarmExecution.getStepExecutions().stream()).toList()) {
            long stepMs = (step.getStartTime() != null && step.getEndTime() != null)
                    ? Duration.between(step.getStartTime(), step.getEndTime()).toMillis()
                    : -1;
            System.out.printf(
                    "[PERF] step=%-28s read=%-6d write=%-6d skip=%-4d commit=%-4d duration=%dms%n",
                    step.getStepName(),
                    step.getReadCount(),
                    step.getWriteCount(),
                    step.getReadSkipCount() + step.getWriteSkipCount() + step.getProcessSkipCount(),
                    step.getCommitCount(),
                    stepMs);
        }

        // 누락 확인: 시드한 유저 수만큼 두 Step 모두 빠짐없이 처리됐는지
        StepExecution curationStep = findStep(curationExecution, "generateMonthlyCurationStep");
        StepExecution alarmStep = findStep(alarmExecution, "sendCurationAlarmStep");

        assertThat(curationStep.getReadCount()).isEqualTo(USER_COUNT);
        assertThat(curationStep.getWriteCount()).isEqualTo(USER_COUNT);
        assertThat(alarmStep.getReadCount()).isEqualTo(USER_COUNT);
        assertThat(alarmStep.getWriteCount()).isEqualTo(USER_COUNT);
    }

    private StepExecution findStep(JobExecution execution, String stepName) {
        return execution.getStepExecutions().stream()
                .filter(s -> s.getStepName().equals(stepName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(stepName + " step execution not found"));
    }

    // AlarmSetting이 @MapsId로 Users를 참조하므로, 같은 트랜잭션 안에서 함께 저장해야 함
    private void seedUsers(int count) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            List<Users> users = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                users.add(Users.builder().nickName("perf-user-" + i).password("pw").build());
            }
            List<Users> savedUsers = userRepository.saveAll(users);

            List<AlarmSetting> settings = savedUsers.stream()
                    .map(AlarmSetting::createDefault)
                    .toList();
            alarmSettingRepository.saveAll(settings);
        });
    }

    private JobParameters uniqueJobParameters() {
        return new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
    }
}
