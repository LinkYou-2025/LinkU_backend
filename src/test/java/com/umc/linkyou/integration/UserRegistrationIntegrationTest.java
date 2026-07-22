package com.umc.linkyou.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Interests;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.classification.Purposes;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.oauth2.utils.UserSocialLoginHelper;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.classification.InterestRepository;
import com.umc.linkyou.repository.classification.JobRepository;
import com.umc.linkyou.repository.classification.PurposeRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.support.config.TestExternalConfig;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(
        properties = {
            "spring.datasource.url=jdbc:tc:postgresql:16:///userregistrationtest",
            "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
            "spring.datasource.username=test",
            "spring.datasource.password=test",
            "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
            "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.sql.init.mode=never"
        })
@Import(TestExternalConfig.class)
@Transactional
public class UserRegistrationIntegrationTest {
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserService userService;
    @Autowired private UserSocialLoginHelper socialLoginHelper;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private JobRepository jobRepository;
    @Autowired private InterestRepository interestRepository;
    @Autowired private PurposeRepository purposeRepository;
    @Autowired private UserRepository userRepository;

    // ddl-auto=create-drop + sql.init.mode=never 라 Flyway(V9) 시딩이 적용되지 않는다.
    // resolvePurposes/resolveInterests가 카탈로그를 조회하므로, 테스트에서 쓰는 값만 직접 시딩한다.
    @BeforeEach
    void seedClassificationCatalog() {
        interestRepository.save(Interests.of("IT"));
        purposeRepository.save(Purposes.of("STUDY"));
    }

    @Test
    @DisplayName("일반 가입 -> 동일 이메일 소셜 로그인 -> 계정 통합 전체 흐름 검증")
    void fullRegistrationAndLinkingFlow() {
        Job testJob = jobRepository.save(Job.builder().name("테스트직업").build());

        // 1. 일반 회원 가입 (JoinDTO가 record이므로 Builder 또는 전체 생성자 사용)
        // DTO에 @Builder를 붙였다고 가정 시:
        UserRequestDTO.JoinDTO joinReq =
                UserRequestDTO.JoinDTO.builder()
                        .email("integration@test.com")
                        .nickName("통합유저")
                        .password("pass123")
                        .gender(Gender.MALE)
                        .jobId(testJob.getId())
                        .purposeList(List.of("STUDY"))
                        .interestList(List.of("IT"))
                        .termsMap(Map.of(TermsType.TERMS_OF_USE, true))
                        .deviceId("test-device-1")
                        .deviceType(DeviceType.PHONE)
                        .build();

        UserResponseDTO.JoinResultDTO generalResult = userService.joinUser(joinReq);

        // 2. 동일 이메일로 소셜 로그인 시도
        Users linkedUser =
                socialLoginHelper.findOrCreateUser(
                        "integration@test.com", "카카오이름", "kakao_unique_id", "url", Provider.KAKAO);

        // 3. 최종 검증
        assertEquals(generalResult.getUserId(), linkedUser.getId(), "유저 ID가 동일해야 함 (계정 통합)");

        long accountCount =
                authAccountRepository.findAll().stream()
                        .filter(a -> a.getUser().getId().equals(linkedUser.getId()))
                        .count();
        assertEquals(2, accountCount, "한 유저에게 GENERAL, KAKAO 두 개의 계정이 연결되어야 함");
    }

    @Test
    @DisplayName("소셜 프로필 완성 시 이미 ACTIVE 상태인 경우 예외 발생 검증")
    void socialProfileCompleteFailWhenAlreadyActive() {
        Job testJob = jobRepository.save(Job.builder().name("테스트직업").build());

        // 1. 이미 가입된(ACTIVE) 유저 생성
        UserRequestDTO.JoinDTO joinReq =
                UserRequestDTO.JoinDTO.builder()
                        .email("already@active.com")
                        .nickName("기존유저")
                        .password("pass123")
                        .gender(Gender.MALE)
                        .jobId(testJob.getId())
                        .purposeList(List.of("STUDY"))
                        .interestList(List.of("IT"))
                        .termsMap(Map.of(TermsType.TERMS_OF_USE, true))
                        .deviceId("test-device-2")
                        .deviceType(DeviceType.PHONE)
                        .build();

        UserResponseDTO.JoinResultDTO joinResult = userService.joinUser(joinReq);
        Users activeUser = userRepository.findById(joinResult.getUserId()).orElseThrow();
        assertEquals(UserStatus.ACTIVE, activeUser.getStatus());

        // 2. 소셜 프로필 완성 DTO (일반 클래스이므로 기존 생성자 유지)
        UserRequestDTO.SocialCompleteDTO completeReq =
                new UserRequestDTO.SocialCompleteDTO(
                        "새닉네임",
                        Gender.FEMALE,
                        testJob.getId(),
                        List.of("WORK"),
                        List.of("ART"),
                        Collections.emptyMap());

        // 3. 예외 발생 검증
        com.umc.linkyou.apiPayload.exception.handler.UserHandler exception =
                assertThrows(
                        com.umc.linkyou.apiPayload.exception.handler.UserHandler.class,
                        () -> userService.socialCompleteProfile(activeUser.getId(), completeReq));

        // getCode() 메서드를 통해 에러 코드 검증
        assertEquals(UserErrorStatus._DUPLICATE_JOIN_REQUEST, exception.getCode());
    }
}
