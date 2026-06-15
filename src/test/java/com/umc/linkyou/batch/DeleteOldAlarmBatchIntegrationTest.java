package com.umc.linkyou.batch;

import com.umc.linkyou.domain.Alarm;
import com.umc.linkyou.domain.UserAlarm;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.repository.AlarmRepository;
import com.umc.linkyou.repository.UserAlarmRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:alarm-batch-test;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.enabled=false",
        "spring.sql.init.mode=never"
})
@DisplayName("DeleteOldAlarmJob 통합 테스트")
class DeleteOldAlarmBatchIntegrationTest {

    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobRepository jobRepository;
    @Autowired @Qualifier("deleteOldAlarmJob") private Job deleteOldAlarmJob;

    @Autowired private AlarmRepository alarmRepository;
    @Autowired private UserAlarmRepository userAlarmRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private JobLauncherTestUtils jobLauncherTestUtils;
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(deleteOldAlarmJob);

        jobRepositoryTestUtils = new JobRepositoryTestUtils(jobRepository);
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM user_alarm");
        jdbcTemplate.execute("DELETE FROM alarms");
        jdbcTemplate.execute("DELETE FROM users");
    }

    // ── 삭제 조건 ────────────────────────────────────────────

    @Nested
    @DisplayName("삭제 조건")
    class 삭제조건 {

        @Test
        @DisplayName("90일 초과 알람과 연결된 사용자 알람이 함께 삭제된다")
        void 90일초과_알람_UserAlarm_함께_삭제() throws Exception {
            // given
            Users user = saveUser("alarm-test-user");
            Alarm oldAlarm    = saveAlarm(AlarmType.CURATION_UPDATED, 1L, LocalDateTime.now().minusDays(91));
            Alarm recentAlarm = saveAlarm(AlarmType.CURATION_UPDATED, 2L, LocalDateTime.now().minusDays(30));
            userAlarmRepository.save(UserAlarm.create(user, oldAlarm));
            userAlarmRepository.save(UserAlarm.create(user, recentAlarm));

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(alarmRepository.findById(oldAlarm.getId())).isEmpty();
            assertThat(alarmRepository.findById(recentAlarm.getId())).isPresent();
        }
    }

    // ── 보존 조건 ────────────────────────────────────────────

    @Nested
    @DisplayName("보존 조건")
    class 보존조건 {

        @Test
        @DisplayName("90일 이내 알람은 삭제되지 않는다")
        void 90일이내_알람_보존() throws Exception {
            // given
            Users user = saveUser("recent-alarm-user");
            Alarm recentAlarm = saveAlarm(AlarmType.CURATION_UPDATED, 3L, LocalDateTime.now().minusDays(89));
            userAlarmRepository.save(UserAlarm.create(user, recentAlarm));

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(alarmRepository.count()).isEqualTo(1);
        }
    }

    // ── 엣지 케이스 ────────────────────────────────────────────

    @Nested
    @DisplayName("엣지 케이스")
    class 엣지케이스 {

        @Test
        @DisplayName("삭제 대상이 없으면 잡이 정상 완료된다")
        void 삭제대상_없음_COMPLETED() throws Exception {
            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(alarmRepository.count()).isEqualTo(0);
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────

    private Users saveUser(String nickName) {
        return userRepository.save(Users.builder().nickName(nickName).password("pw").build());
    }

    private Alarm saveAlarm(AlarmType type, Long targetId, LocalDateTime createdAt) {
        Alarm saved = alarmRepository.save(Alarm.create(type, targetId));
        jdbcTemplate.update(
                "UPDATE alarms SET created_at = ? WHERE alarm_id = ?",
                createdAt, saved.getId()
        );
        return alarmRepository.findById(saved.getId()).orElseThrow();
    }

    private JobParameters uniqueJobParameters() {
        return new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
    }
}
