package com.umc.linkyou.batch;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.UsersFcmToken;
import com.umc.linkyou.repository.UserFcmTokenRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import com.umc.linkyou.support.config.TestExternalConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
@TestPropertySource(properties = {
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.enabled=false"
})
@DisplayName("DeleteExpiredFcmTokenJob 통합 테스트")
class DeleteExpiredFcmTokenBatchIntegrationTest {

    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobRepository jobRepository;
    @Autowired @Qualifier("deleteExpiredFcmTokenJob") private Job deleteExpiredFcmTokenJob;

    @Autowired private UserFcmTokenRepository userFcmTokenRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private JobLauncherTestUtils jobLauncherTestUtils;
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(deleteExpiredFcmTokenJob);

        jobRepositoryTestUtils = new JobRepositoryTestUtils(jobRepository);
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.execute("DELETE FROM user_fcm_tokens");
        jdbcTemplate.execute("DELETE FROM users");
    }

    // ── 삭제 조건 ────────────────────────────────────────────

    @Nested
    @DisplayName("삭제 조건")
    class DeleteCondition {

        @Test
        @DisplayName("270일 초과된 FCM 토큰이 삭제되고 유효 토큰은 보존된다")
        void 만료토큰_삭제_유효토큰_보존() throws Exception {
            // given
            Users user = saveUser("fcm-test-user");
            UsersFcmToken expiredToken = saveFcmToken(user, "expired-token", LocalDateTime.now().minusDays(271));
            UsersFcmToken activeToken  = saveFcmToken(user, "active-token",  LocalDateTime.now().minusDays(10));

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(userFcmTokenRepository.findById(expiredToken.getId())).isEmpty();
            assertThat(userFcmTokenRepository.findById(activeToken.getId())).isPresent();
        }

        @Test
        @DisplayName("lastUsedAt이 270일 1초 전이면 삭제된다")
        void lastUsedAt_270일_1초전_삭제() throws Exception {
            // given — WHERE lastUsedAt < cutoff: cutoff보다 1초 이전 → 삭제 대상
            Users user = saveUser("just-over-cutoff-user");
            UsersFcmToken expiredToken = saveFcmToken(user, "just-expired-token",
                    LocalDateTime.now().minusDays(270).minusSeconds(1));

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(userFcmTokenRepository.findById(expiredToken.getId())).isEmpty();
        }
    }

    // ── 보존 조건 ────────────────────────────────────────────

    @Nested
    @DisplayName("보존 조건")
    class PreserveCondition {

        @Test
        @DisplayName("lastUsedAt이 270일 경계 이내(+30초)이면 삭제되지 않는다")
        void lastUsedAt_경계이내_보존() throws Exception {
            // given — WHERE lastUsedAt < cutoff: cutoff + 30초는 strict-< 조건 불충족 → 보존
            //          테스트 셋업 시점과 Job이 실제 cutoff를 계산하는 시점 사이의 시간차(CI처럼 느린
            //          환경에서는 1초를 넘을 수 있음)를 흡수하기 위해 30초 여유를 둔다
            Users user = saveUser("at-cutoff-user");
            UsersFcmToken boundaryToken = saveFcmToken(user, "boundary-token",
                    LocalDateTime.now().minusDays(270).plusSeconds(30));

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(userFcmTokenRepository.findById(boundaryToken.getId())).isPresent();
        }
    }

    // ── 엣지 케이스 ────────────────────────────────────────────

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCase {

        @Test
        @DisplayName("삭제 대상이 없으면 잡이 정상 완료된다")
        void 삭제대상_없음_COMPLETED() throws Exception {
            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(userFcmTokenRepository.count()).isEqualTo(0);
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────

    private Users saveUser(String nickName) {
        return userRepository.save(Users.builder().nickName(nickName).password("pw").build());
    }

    private UsersFcmToken saveFcmToken(Users user, String token, LocalDateTime lastUsedAt) {
        UsersFcmToken saved = userFcmTokenRepository.save(
                UsersFcmToken.builder().user(user).fcmToken(token).build());
        jdbcTemplate.update(
                "UPDATE user_fcm_tokens SET last_used_at = ? WHERE user_fcm_token_id = ?",
                lastUsedAt, saved.getId()
        );
        return userFcmTokenRepository.findById(saved.getId()).orElseThrow();
    }

    private JobParameters uniqueJobParameters() {
        return new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
    }
}
