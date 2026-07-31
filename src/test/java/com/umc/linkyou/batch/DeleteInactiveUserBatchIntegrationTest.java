package com.umc.linkyou.batch;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.jwt.RefreshTokenManager;
import com.umc.linkyou.repository.redis.FcmTokenRedisRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
@TestPropertySource(properties = {
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.enabled=false"
})
@DisplayName("DeleteInactiveUserJob 통합 테스트")
class DeleteInactiveUserBatchIntegrationTest {

    @Autowired private JobLauncher jobLauncher;
    @Autowired private JobRepository jobRepository;
    @Autowired @Qualifier("deleteInactiveUserJob") private Job deleteInactiveUserJob;

    @Autowired private UserRepository userRepository;

    // Writer 내부 Redis 호출 — 실제 연결 없이 동작하도록 Mock 처리
    @MockitoBean private RefreshTokenManager refreshTokenManager;
    @MockitoBean private FcmTokenRedisRepository fcmTokenRedisRepository;

    private JobLauncherTestUtils jobLauncherTestUtils;
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils = new JobLauncherTestUtils();
        jobLauncherTestUtils.setJobLauncher(jobLauncher);
        jobLauncherTestUtils.setJobRepository(jobRepository);
        jobLauncherTestUtils.setJob(deleteInactiveUserJob);

        jobRepositoryTestUtils = new JobRepositoryTestUtils(jobRepository);
        jobRepositoryTestUtils.removeJobExecutions();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    // ── 삭제 조건 ────────────────────────────────────────────

    @Nested
    @DisplayName("삭제 조건")
    class DeleteCondition {

        @Test
        @DisplayName("INACTIVE 상태이고 inactiveDate가 14일 초과된 사용자는 삭제되고, 미만이거나 ACTIVE인 사용자는 보존된다")
        void INACTIVE_14일초과_삭제_그외_보존() throws Exception {
            // given
            Users deleteTarget = saveUser("delete-target", UserStatus.INACTIVE, LocalDateTime.now().minusDays(15));
            Users keepRecent   = saveUser("keep-recent",   UserStatus.INACTIVE, LocalDateTime.now().minusDays(10));
            Users keepActive   = saveUser("keep-active",   UserStatus.ACTIVE,   null);

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(userRepository.findById(deleteTarget.getId())).isEmpty();
            assertThat(userRepository.findById(keepRecent.getId())).isPresent();
            assertThat(userRepository.findById(keepActive.getId())).isPresent();
        }

        @Test
        @DisplayName("inactiveDate가 정확히 14일 전(경계값)이면 삭제된다")
        void inactiveDate_정확히_14일_전_경계값_삭제() throws Exception {
            // given — inactiveDate == now() - 14일: loe(cutoff) 조건에 해당하므로 삭제 대상
            Users boundaryUser = saveUser("boundary-user", UserStatus.INACTIVE,
                    LocalDateTime.now().minusDays(14));

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(userRepository.findById(boundaryUser.getId())).isEmpty();
        }
    }

    // ── 보존 조건 ────────────────────────────────────────────

    @Nested
    @DisplayName("보존 조건")
    class PreserveCondition {

        @Test
        @DisplayName("inactiveDate가 13일 전이면 유예기간 내이므로 삭제되지 않는다")
        void inactiveDate_13일_전_유예기간_보존() throws Exception {
            // given — inactiveDate == now() - 13일: loe(cutoff) 조건 불충족 → 보존
            Users recentInactive = saveUser("recent-inactive", UserStatus.INACTIVE,
                    LocalDateTime.now().minusDays(13));

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(userRepository.findById(recentInactive.getId())).isPresent();
        }

        @Test
        @DisplayName("inactiveDate가 null이면 삭제되지 않는다")
        void inactiveDate_null_보존() throws Exception {
            // given
            Users user = saveUser("null-date-user", UserStatus.INACTIVE, null);

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(userRepository.findById(user.getId())).isPresent();
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
            StepExecution step = execution.getStepExecutions().iterator().next();
            assertThat(step.getReadCount()).isEqualTo(0);
            assertThat(step.getWriteCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("afterCommit Redis 실패해도 배치는 COMPLETED이고 DB 삭제는 유지된다")
        void afterCommit_Redis_실패해도_COMPLETED_DB_삭제_유지() throws Exception {
            // given
            Users deleteTarget = saveUser("redis-fail-user", UserStatus.INACTIVE,
                    LocalDateTime.now().minusDays(15));

            doThrow(new RuntimeException("Redis connection refused"))
                    .when(refreshTokenManager).deleteAllTokens(anyLong());

            // when
            JobExecution execution = jobLauncherTestUtils.launchJob(uniqueJobParameters());

            // then — Redis 실패는 afterCommit 내에서 catch되므로 배치 상태에 영향 없음
            assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(userRepository.findById(deleteTarget.getId())).isEmpty();
        }
    }

    // ── 헬퍼 ────────────────────────────────────────────

    private Users saveUser(String nickName, UserStatus status, LocalDateTime inactiveDate) {
        return userRepository.save(Users.builder()
                .nickName(nickName)
                .password("pw")
                .status(status)
                .inactiveDate(inactiveDate)
                .build());
    }

    private JobParameters uniqueJobParameters() {
        return new JobParametersBuilder()
                .addLong("run.id", System.currentTimeMillis())
                .toJobParameters();
    }
}
