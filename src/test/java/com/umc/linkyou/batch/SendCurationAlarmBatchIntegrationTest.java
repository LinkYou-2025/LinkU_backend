package com.umc.linkyou.batch;

import com.umc.linkyou.domain.Alarm;
import com.umc.linkyou.domain.AlarmSetting;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.UserAlarm;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.repository.AlarmRepository;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.repository.UserAlarmRepository;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
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

import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
@TestPropertySource(properties = {
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.enabled=false"
})
@DisplayName("sendCurationAlarmStep 통합 테스트")
class SendCurationAlarmBatchIntegrationTest {

    private static final String BASE_MONTH = YearMonth.now().minusMonths(1).toString();

    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobRepository jobRepository;
    @Autowired @Qualifier("sendCurationAlarmJob") private Job sendCurationAlarmJob;

    @Autowired private UserRepository userRepository;
    @Autowired private CurationRepository curationRepository;
    @Autowired private AlarmRepository alarmRepository;
    @Autowired private UserAlarmRepository userAlarmRepository;
    @Autowired private AlarmSettingRepository alarmSettingRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    // FcmServiceImpl은 FcmPushSender/FcmSubscriber 양쪽 인터페이스를 구현하므로,
    // 구현 타입 기준으로 mock해야 FcmSubscriber를 요구하는 다른 빈(AlarmSettingEventListener)도 정상 주입됨
    @MockitoBean private FcmServiceImpl fcmServiceImpl;

    private JobLauncherTestUtils jobLauncherTestUtils;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(sendCurationAlarmJob);

        JobRepositoryTestUtils jobRepositoryTestUtils = new JobRepositoryTestUtils(jobRepository);
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM user_alarms");
        jdbcTemplate.execute("DELETE FROM alarms");
        jdbcTemplate.execute("DELETE FROM curations");
        jdbcTemplate.execute("DELETE FROM alarm_settings");
        jdbcTemplate.execute("DELETE FROM users");
    }

    // ── 정상 처리 ────────────────────────────────────────────

    @Nested
    @DisplayName("정상 처리")
    class NormalCase {

        @Test
        @DisplayName("알림 설정이 켜진 유저의 큐레이션에 개인화된 알림이 저장되고 일괄 발송된다")
        void 알림설정_켜진_유저_알림_발송() throws Exception {
            // given
            Users user = saveUserWithSetting("유저에이", true);
            Curation curation = saveCuration(user, BASE_MONTH);

            // when
            JobExecution execution = jobLauncherTestUtils.launchStep("sendCurationAlarmStep", uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

            List<Alarm> alarms = alarmRepository.findAll();
            assertThat(alarms).hasSize(1);
            assertThat(alarms.get(0).getBody()).isEqualTo("유저에이님을 위한 이 달의 큐레이션이 도착했어요!");
            assertThat(alarms.get(0).getTargetId()).isEqualTo(curation.getCurationId());
            assertThat(userAlarmRepository.count()).isEqualTo(1);

            verify(fcmServiceImpl, timeout(3000)).sendBulkPersonalized(anyList());
        }
    }

    // ── 재실행(멱등) ────────────────────────────────────────────

    @Nested
    @DisplayName("재실행 시 멱등성")
    class Idempotency {

        @Test
        @DisplayName("이미 알림이 발송된 큐레이션은 재실행해도 다시 대상이 되지 않는다")
        void 이미_발송된_큐레이션_재대상_아님() throws Exception {
            // given
            Users user = saveUserWithSetting("유저비", true);
            Curation curation = saveCuration(user, BASE_MONTH);
            Alarm existing = alarmRepository.save(
                    Alarm.create(AlarmType.CURATION_UPDATED, curation.getCurationId(), "이미 보낸 알림"));
            userAlarmRepository.save(UserAlarm.create(user, existing));

            // when
            JobExecution execution = jobLauncherTestUtils.launchStep("sendCurationAlarmStep", uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            StepExecution step = execution.getStepExecutions().iterator().next();
            assertThat(step.getReadCount()).isEqualTo(0);
            assertThat(alarmRepository.count()).isEqualTo(1);
        }
    }

    // ── 엣지 케이스 ────────────────────────────────────────────

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCase {

        @Test
        @DisplayName("큐레이션 알림 설정이 꺼진 유저는 알림이 발송되지 않는다")
        void 알림설정_꺼진_유저_스킵() throws Exception {
            // given
            Users user = saveUserWithSetting("유저씨", false);
            saveCuration(user, BASE_MONTH);

            // when
            JobExecution execution = jobLauncherTestUtils.launchStep("sendCurationAlarmStep", uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(alarmRepository.count()).isEqualTo(0);
            assertThat(userAlarmRepository.count()).isEqualTo(0);
            verify(fcmServiceImpl, never()).sendBulkPersonalized(anyList());
        }

        @Test
        @DisplayName("지난달이 아닌 큐레이션은 대상이 되지 않는다")
        void 지난달_아닌_큐레이션_제외() throws Exception {
            // given
            Users user = saveUserWithSetting("유저디", true);
            saveCuration(user, YearMonth.now().minusMonths(2).toString());

            // when
            JobExecution execution = jobLauncherTestUtils.launchStep("sendCurationAlarmStep", uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(alarmRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("대상 큐레이션이 없으면 잡이 정상 완료된다")
        void 대상없음_정상완료() throws Exception {
            // when
            JobExecution execution = jobLauncherTestUtils.launchStep("sendCurationAlarmStep", uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            StepExecution step = execution.getStepExecutions().iterator().next();
            assertThat(step.getReadCount()).isEqualTo(0);
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────

    // 알림 설정과 함께 유저 생성
    private Users saveUserWithSetting(String nickName, boolean curationEnabled) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            Users user = userRepository.save(Users.builder().nickName(nickName).password("pw").build());
            AlarmSetting setting = AlarmSetting.createDefault(user);
            if (!curationEnabled) {
                setting.updateCuration(false);
            }
            alarmSettingRepository.save(setting);
            return user;
        });
    }

    private Curation saveCuration(Users user, String baseMonth) {
        return curationRepository.save(Curation.builder().user(user).baseMonth(baseMonth).build());
    }

    private JobParameters uniqueJobParameters() {
        return new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
    }
}
