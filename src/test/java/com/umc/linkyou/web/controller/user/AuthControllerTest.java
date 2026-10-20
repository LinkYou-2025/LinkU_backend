package com.umc.linkyou.web.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.TokenIssueService;
import com.umc.linkyou.service.email.EmailVerificationService;
import com.umc.linkyou.service.email.PasswordResetService;
import com.umc.linkyou.service.users.TermsAgreementService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.support.fixture.SocialSignupFixture;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.user.UserRequestDTO;
import com.umc.linkyou.web.dto.user.UserResponseDTO;

@WebMvcTest(AuthController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    @MockitoBean private AccessTokenBlackListManager accessTokenBlackListManager;

    @MockitoBean private TermsAgreementService termsAgreementService;

    @MockitoBean private EmailVerificationService emailVerificationService;

    @MockitoBean private PasswordResetService passwordResetService;

    @MockitoBean private UserWithdrawService userWithdrawService;

    @MockitoBean private TokenIssueService tokenIssueService;

    @Nested
    @DisplayName("닉네임 중복 확인")
    class CheckNickname {
        @Nested
        @DisplayName("성공")
        class Success {
            @Test
            @WithCustomUser(userId = 1L)
            @DisplayName("로그인 상태에서 조회 시 본인 식별자를 전달한다")
            void 로그인_상태에서_조회_시_본인_식별자를_전달한다() throws Exception {
                mockMvc.perform(
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .get("/api/v1/auth/check-nickname")
                                        .param("nickname", SocialSignupFixture.NICKNAME))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("AUTH2005"))
                        .andExpect(jsonPath("$.result").isMap());
                org.mockito.Mockito.verify(userService)
                        .checkNicknameAvailable(SocialSignupFixture.NICKNAME, 1L);
            }

            @Test
            @DisplayName("비로그인 상태에서 조회 시 전체 사용자를 기준으로 검사한다")
            void 비로그인_상태에서_조회_시_전체_사용자를_기준으로_검사한다() throws Exception {
                mockMvc.perform(
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .get("/api/v1/auth/check-nickname")
                                        .param("nickname", SocialSignupFixture.NICKNAME))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("AUTH2005"))
                        .andExpect(jsonPath("$.result").isMap());
                org.mockito.Mockito.verify(userService)
                        .checkNicknameAvailable(SocialSignupFixture.NICKNAME, null);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {
            @Test
            @WithCustomUser(userId = 1L)
            @DisplayName("다른 사용자의 닉네임 조회 시 중복 응답을 반환한다")
            void 다른_사용자의_닉네임_조회_시_중복_응답을_반환한다() throws Exception {
                org.mockito.Mockito.doThrow(new UserHandler(UserErrorStatus._DUPLICATE_NICKNAME))
                        .when(userService)
                        .checkNicknameAvailable(SocialSignupFixture.NICKNAME, 1L);
                mockMvc.perform(
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                        .get("/api/v1/auth/check-nickname")
                                        .param("nickname", SocialSignupFixture.NICKNAME))
                        .andExpect(status().isConflict())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value("USERS4091"))
                        .andExpect(jsonPath("$.result").exists());
            }
        }
    }

    @Nested
    @DisplayName("회원가입 엔드포인트")
    class Join {

        @Test
        @DisplayName("성공 - 회원가입 요청을 처리하고 생성 결과를 반환한다")
        void join_user_docs() throws Exception {
            UserRequestDTO.JoinDTO request =
                    new UserRequestDTO.JoinDTO(
                            "링큐유저",
                            "test@example.com",
                            "password123",
                            Gender.MALE,
                            1L,
                            List.of("CAREER"),
                            List.of("IT"),
                            Map.of(
                                    TermsType.PRIVACY_POLICY, true,
                                    TermsType.TERMS_OF_USE, true),
                            "ios-iphone-16-pro",
                            DeviceType.PHONE);

            UserResponseDTO.JoinResultDTO mockResult =
                    UserResponseDTO.JoinResultDTO.builder()
                            .userId(1L)
                            .createdAt(LocalDateTime.now())
                            .tokenResponse(
                                    new UserResponseDTO.TokenPair("mockAccess", "mockRefresh"))
                            .build();

            given(userService.joinUser(any())).willReturn(mockResult);

            mockMvc.perform(
                            post("/api/v1/auth/signup")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request))
                                    .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true));
        }
    }
}
