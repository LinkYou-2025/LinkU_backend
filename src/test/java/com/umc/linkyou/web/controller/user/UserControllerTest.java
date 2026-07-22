package com.umc.linkyou.web.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.enums.TermsType;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.SecurityErrorResponseWriter;
import com.umc.linkyou.service.email.EmailVerificationService;
import com.umc.linkyou.service.email.PasswordResetService;
import com.umc.linkyou.service.users.TermsAgreementService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;

    @MockitoBean
    private TermsAgreementService termsAgreementService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private UserWithdrawService userWithdrawService;

    @MockitoBean
    private SecurityErrorResponseWriter securityErrorResponseWriter;

    @Nested
    @DisplayName("소셜 프로필 완성 엔드포인트")
    class CompleteSocialProfile {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("성공 - 소셜 프로필 완성 요청을 처리하고 가입 결과를 반환한다")
            @WithCustomUser(userId = 2L)
            void social_complete_success() throws Exception {
                UserRequestDTO.SocialCompleteDTO request = new UserRequestDTO.SocialCompleteDTO(
                        "social_nick",
                        Gender.MALE,
                        1L,
                        List.of("STUDY"),
                        List.of("DESIGN"),
                        Map.of(TermsType.TERMS_OF_USE, true),
                        "ios-iphone-16-pro",
                        DeviceType.PHONE
                );

                UserResponseDTO.JoinResultDTO mockResult = UserResponseDTO.JoinResultDTO.builder()
                        .userId(2L)
                        .createdAt(LocalDateTime.now())
                        .tokenResponse(new UserResponseDTO.TokenPair("mockAccess", "mockRefresh"))
                        .build();

                given(userService.socialCompleteProfile(any(), any(), any())).willReturn(mockResult);

                mockMvc.perform(patch("/api/v1/users/social/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("실패 - 비인증 사용자가 요청 시 401 에러를 반환한다")
            void social_complete_unauthorized() throws Exception {
                UserRequestDTO.SocialCompleteDTO request = new UserRequestDTO.SocialCompleteDTO(
                        "social_nick",
                        Gender.MALE,
                        1L,
                        List.of("STUDY"),
                        List.of("DESIGN"),
                        Map.of(TermsType.MARKETING, true),
                        "ios-iphone-16-pro",
                        DeviceType.PHONE
                );

                mockMvc.perform(patch("/api/v1/users/social/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("약관 상태 조회 엔드포인트")
    class GetTermsStatus {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("성공 - 현재 로그인한 사용자의 약관 동의 상태를 반환한다")
            @WithCustomUser(userId = 1L)
            void get_terms_status_success() throws Exception {
                UserResponseDTO.TermsStatusDTO mockResponse = UserResponseDTO.TermsStatusDTO.builder()
                        .userId(1L)
                        .termsStatus(Map.of(
                                TermsType.TERMS_OF_USE, true,
                                TermsType.PRIVACY_POLICY, true,
                                TermsType.MARKETING, false
                        ))
                        .allRequiredAgreed(true)
                        .build();

                given(termsAgreementService.getTermsStatus(any())).willReturn(mockResponse);

                mockMvc.perform(get("/api/v1/users/terms/status")
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("실패 - 비인증 사용자가 조회 시 401 에러를 반환한다")
            void get_terms_status_unauthorized() throws Exception {
                mockMvc.perform(get("/api/v1/users/terms/status")
                                .accept(MediaType.APPLICATION_JSON))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("약관 일괄 업데이트 엔드포인트")
    class UpdateTermsAgree {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("성공 - 약관 동의 맵을 받아 정보를 업데이트하고 결과를 반환한다")
            @WithCustomUser(userId = 1L)
            void update_terms_success() throws Exception {
                UserRequestDTO.TermsAgreeDTO request = UserRequestDTO.TermsAgreeDTO.builder()
                        .termsMap(Map.of(TermsType.MARKETING, true))
                        .termsVersion("v1.0")
                        .build();

                UserResponseDTO.TermsStatusDTO mockResponse = UserResponseDTO.TermsStatusDTO.builder()
                        .userId(1L)
                        .termsStatus(Map.of(
                                TermsType.TERMS_OF_USE, true,
                                TermsType.PRIVACY_POLICY, true,
                                TermsType.MARKETING, true
                        ))
                        .allRequiredAgreed(true)
                        .build();

                given(termsAgreementService.updateTermsAgree(any(), any())).willReturn(mockResponse);

                mockMvc.perform(patch("/api/v1/users/terms/agree")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("실패 - 유효하지 않은 약관 타입이 포함된 경우 Jackson 역직렬화 시점에 400을 반환한다")
            @WithCustomUser(userId = 1L)
            void update_terms_fail_invalid_type() throws Exception {
                String invalidJson = """
                        {
                          "termsMap": { "INVALID_TYPE": true },
                          "termsVersion": "v1.0"
                        }
                        """;

                mockMvc.perform(patch("/api/v1/users/terms/agree")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                                .with(csrf()))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("실패 - 비인증 사용자가 요청 시 401 에러를 반환한다")
            void update_terms_unauthorized() throws Exception {
                UserRequestDTO.TermsAgreeDTO request = UserRequestDTO.TermsAgreeDTO.builder()
                        .termsMap(Map.of(TermsType.MARKETING, true))
                        .termsVersion("v1.0")
                        .build();

                mockMvc.perform(patch("/api/v1/users/terms/agree")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    @Nested
    @DisplayName("계정 즉시 완전 삭제 엔드포인트 (테스트용)")
    class TestDeleteInactive {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("성공 - 로그인한 본인 계정을 즉시 삭제한다")
            @WithCustomUser(userId = 3L)
            void test_delete_inactive_success() throws Exception {
                Users mockUser = Users.builder()
                        .id(3L)
                        .nickName("삭제될유저")
                        .status(UserStatus.INACTIVE)
                        .build();
                ReflectionTestUtils.setField(mockUser, "createdAt", LocalDateTime.now());

                given(userWithdrawService.testImmediateDelete(3L)).willReturn(mockUser);

                mockMvc.perform(post("/api/v1/users/test/delete-inactive")
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.result.userId").value(3));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("실패 - 비인증 사용자가 요청 시 401 에러를 반환한다")
            void test_delete_inactive_unauthorized() throws Exception {
                mockMvc.perform(post("/api/v1/users/test/delete-inactive")
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }

            @Test
            @DisplayName("실패 - 존재하지 않는 유저인 경우 예외를 반환한다")
            @WithCustomUser(userId = 99L)
            void test_delete_inactive_user_not_found() throws Exception {
                given(userWithdrawService.testImmediateDelete(99L))
                        .willThrow(new UserHandler(UserErrorStatus._USER_NOT_FOUND));

                mockMvc.perform(post("/api/v1/users/test/delete-inactive")
                                .with(csrf()))
                        .andExpect(jsonPath("$.isSuccess").value(false));
            }
        }
    }
}
