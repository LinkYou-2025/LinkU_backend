package com.umc.linkyou.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.umc.linkyou.domain.LinkuSearchHistory;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Interests;
import com.umc.linkyou.domain.classification.Purposes;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.domain.mapping.UsersInterest;
import com.umc.linkyou.domain.mapping.UsersPurpose;
import com.umc.linkyou.repository.LinkuSearchHistoryRepository;
import com.umc.linkyou.repository.classification.InterestRepository;
import com.umc.linkyou.repository.classification.PurposeRepository;
import com.umc.linkyou.repository.classification.UsersInterestRepository;
import com.umc.linkyou.repository.classification.UsersPurposeRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.support.config.TestExternalConfig;

/**
 * V9/V10 flyway 마이그레이션으로 추가한 ON DELETE CASCADE 가 "실제 DB 제약조건"으로
 * 정확히 반영되었는지 검증한다.
 *
 * <p>다른 테스트들은 {@code spring.jpa.hibernate.ddl-auto=create-drop} (엔티티 매핑 기반 스키마)을 쓰지만,
 * {@link LinkuSearchHistory} 는 Users 에 대한 JPA 연관관계 없이 순수 컬럼(userId)으로만 존재해서
 * 엔티티 기반 스키마에는 애초에 FK 자체가 생성되지 않는다. 실제 운영과 동일하게 flyway 마이그레이션을
 * 그대로 적용한 스키마(ddl-auto=validate)에서 검증해야만 이번에 추가한 CASCADE 를 확인할 수 있다.
 */
@ActiveProfiles("test")
@SpringBootTest
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:tc:postgresql:16:///cascadedeletetest",
            "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
            "spring.datasource.username=test",
            "spring.datasource.password=test",
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
            "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
            "spring.flyway.enabled=true",
            "spring.flyway.locations=classpath:db/migration",
            "spring.sql.init.mode=never"
        })
@Import(TestExternalConfig.class)
@Transactional
class UserWithdrawCascadeMigrationIntegrationTest {

    @Autowired private UserWithdrawService userWithdrawService;
    @Autowired private UserRepository userRepository;
    @Autowired private LinkuSearchHistoryRepository linkuSearchHistoryRepository;
    @Autowired private InterestRepository interestRepository;
    @Autowired private PurposeRepository purposeRepository;
    @Autowired private UsersInterestRepository usersInterestRepository;
    @Autowired private UsersPurposeRepository usersPurposeRepository;

    @PersistenceContext private EntityManager em;

    @Test
    @DisplayName(
            "실제 flyway 스키마에서 유저 완전 삭제 시 검색 기록(FK가 새로 추가된 테이블)과 관심사·목적 연결이 CASCADE로 삭제된다")
    void 실제_flyway_스키마에서_유저_삭제_시_검색기록과_관심사_목적_연결이_삭제된다() {
        // given
        Users user =
                userRepository.save(
                        Users.builder()
                                .nickName("cascade_migration_user")
                                .password("encoded-password")
                                .status(UserStatus.ACTIVE)
                                .build());
        Long userId = user.getId();

        LinkuSearchHistory history =
                linkuSearchHistoryRepository.save(LinkuSearchHistory.of(userId, "테스트검색어"));
        Long historyId = history.getId();

        Interests interest = interestRepository.save(Interests.of("IT"));
        UsersInterest usersInterest =
                usersInterestRepository.save(UsersInterest.of(user, interest));
        Long usersInterestId = usersInterest.getId();

        Purposes purpose = purposeRepository.save(Purposes.of("STUDY"));
        UsersPurpose usersPurpose = usersPurposeRepository.save(UsersPurpose.of(user, purpose));
        Long usersPurposeId = usersPurpose.getId();

        em.flush();
        em.clear();

        // when
        userWithdrawService.testImmediateDelete(userId);
        em.flush();
        em.clear();

        // then
        assertFalse(userRepository.existsById(userId), "유저가 삭제되지 않았습니다.");
        assertTrue(
                linkuSearchHistoryRepository.findById(historyId).isEmpty(),
                "linku_search_histories 가 CASCADE로 삭제되지 않았습니다 (V10 FK 누락 회귀).");
        assertTrue(
                usersInterestRepository.findById(usersInterestId).isEmpty(),
                "users_interests 가 CASCADE로 삭제되지 않았습니다.");
        assertTrue(
                usersPurposeRepository.findById(usersPurposeId).isEmpty(),
                "users_purposes 가 CASCADE로 삭제되지 않았습니다.");

        // 관심사/목적 마스터(카탈로그) 데이터는 유저 삭제와 무관하게 유지되어야 한다
        assertTrue(interestRepository.existsById(interest.getId()), "관심사 마스터는 유지되어야 합니다.");
        assertTrue(purposeRepository.existsById(purpose.getId()), "목적 마스터는 유지되어야 합니다.");
    }
}
