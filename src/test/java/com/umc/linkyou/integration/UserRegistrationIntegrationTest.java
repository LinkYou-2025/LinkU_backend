package com.umc.linkyou.integration;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth2.UserSocialLoginHelper;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.classification.JobRepository;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.web.dto.UserRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest // 실제 스프링 컨텍스트를 다 띄움
@Transactional  // 테스트가 끝나면 DB 데이터를 자동으로 롤백 (깔끔함 유지)
public class UserRegistrationIntegrationTest {
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @Autowired private UserService userService;
    @Autowired private UserSocialLoginHelper socialLoginHelper;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private JobRepository jobRepository;

    @Test
    @DisplayName("일반 가입 -> 동일 이메일 소셜 로그인 -> 계정 통합 전체 흐름 검증")
    void fullRegistrationAndLinkingFlow() {
        Job testJob = jobRepository.save(Job.builder()
                .name("테스트직업")
                .build());
        // 1. 일반 회원 가입 (GENERAL)
        UserRequestDTO.JoinDTO joinReq = new UserRequestDTO.JoinDTO();
        joinReq.setEmail("integration@test.com");
        joinReq.setNickName("통합유저");
        joinReq.setPassword("pass123");
        joinReq.setGender(1);
        joinReq.setJobId(testJob.getId());// 주의: DB(H2)에 Job ID 1이 있어야 함

        Users generalUser = userService.joinUser(joinReq);

        // 2. 동일 이메일로 소셜 로그인 시도 (KAKAO)
        // 실제 로직이 돌면서 기존 유저를 찾아 AuthAccount만 추가함
        Users linkedUser = socialLoginHelper.findOrCreateUser(
                "integration@test.com", "카카오이름", "kakao_unique_id", "url", Provider.KAKAO);

        // 3. 최종 검증
        assertEquals(generalUser.getId(), linkedUser.getId(), "유저 ID가 동일해야 함 (계정 통합)");

        long accountCount = authAccountRepository.findAll().stream()
                .filter(a -> a.getUser().getId().equals(linkedUser.getId()))
                .count();
        assertEquals(2, accountCount, "한 유저에게 GENERAL, KAKAO 두 개의 계정이 연결되어야 함");
    }

    @Test
    @DisplayName("소셜 프로필 완성 시 이미 ACTIVE 상태인 경우 예외 발생 검증")
    void socialProfileCompleteFailWhenAlreadyActive() {
        // 0. 테스트용 Job 생성
        Job testJob = jobRepository.save(Job.builder().name("테스트직업").build());

        // 1. 이미 가입된(ACTIVE) 유저 생성 (일반 가입 활용)
        UserRequestDTO.JoinDTO joinReq = new UserRequestDTO.JoinDTO();
        joinReq.setEmail("already@active.com");
        joinReq.setNickName("기존유저");
        joinReq.setPassword("pass123");
        joinReq.setGender(1);
        joinReq.setJobId(testJob.getId());
        joinReq.setPurposeList(List.of("STUDY"));
        joinReq.setInterestList(List.of("IT"));

        Users activeUser = userService.joinUser(joinReq);
        assertEquals(UserStatus.ACTIVE, activeUser.getStatus());

        // 2. 이미 ACTIVE인 유저가 소셜 프로필 완성 API 로직 호출
        UserRequestDTO.SocialCompleteDTO completeReq = new UserRequestDTO.SocialCompleteDTO(
                "새닉네임", 2, testJob.getId(), List.of("WORK"), List.of("ART")
        );

        // 3. 예외 발생 검증
        com.umc.linkyou.apiPayload.exception.handler.UserHandler exception = assertThrows(
                com.umc.linkyou.apiPayload.exception.handler.UserHandler.class,
                () -> userService.socialCompleteProfile(activeUser, completeReq)
        );

        // getErrorStatus()를 getErrorCode()로 변경
        assertEquals(ErrorStatus._ALREADY_ACTIVE_USER, exception.getCode());
    }
}
