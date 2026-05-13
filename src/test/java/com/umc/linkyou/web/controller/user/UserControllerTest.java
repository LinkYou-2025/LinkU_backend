package com.umc.linkyou.web.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.CurrentUserArgumentResolver;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.service.email.EmailVerificationService;
import com.umc.linkyou.service.email.PasswordResetService;
import com.umc.linkyou.service.users.TermsAgreementService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import com.umc.linkyou.support.security.WithCustomUser;
import com.umc.linkyou.web.dto.UserRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest({UserController.class, AuthController.class})
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension.class)
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

    @Nested
    @DisplayName("회원가입 엔드포인트")
    class Join {

        @Test
        @DisplayName("성공 - 회원가입 요청을 처리하고 생성 결과를 반환한다")
        void join_user_docs() throws Exception {
            // given
            UserRequestDTO.JoinDTO request = new UserRequestDTO.JoinDTO();
            request.setEmail("test@example.com");
            request.setNickName("링큐유저");
            request.setPassword("password123");
            request.setGender(1);
            request.setJobId(1L);
            request.setPurposeList(List.of("CAREER"));
            request.setInterestList(List.of("IT"));

            Users mockUser = Users.builder()
                    .id(1L)
                    .build();
            mockUser.setCreatedAt(LocalDateTime.now());

            given(userService.joinUser(any())).willReturn(mockUser);

            // when & then
            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("AUTH201"))
                    .andExpect(jsonPath("$.result.userId").value(1L))
                    .andDo(document("user/join",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("email").description("이메일"),
                                    fieldWithPath("nickName").description("닉네임"),
                                    fieldWithPath("password").description("비밀번호"),
                                    fieldWithPath("gender").description("성별 (1:남, 2:여)"),
                                    fieldWithPath("jobId").description("직업 ID"),
                                    fieldWithPath("purposeList").description("가입 목적 리스트"),
                                    fieldWithPath("interestList").description("관심사 리스트")
                            ),
                            responseFields(
                                    fieldWithPath("isSuccess").description("성공 여부"),
                                    fieldWithPath("code").description("응답 코드"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("timestamp").description("응답 시간"),
                                    fieldWithPath("result.userId").description("생성된 유저 ID"),
                                    fieldWithPath("result.createdAt").description("생성 일시")
                            )
                    ));
        }
    }

    @Nested
    @DisplayName("소셜 프로필 완성 엔드포인트")
    class CompleteSocialProfile {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("성공 - 소셜 프로필 완성 요청을 처리하고 가입 결과를 반환한다")
            @WithCustomUser(userId = 2L)
            void social_complete_docs() throws Exception {
                // given
                UserRequestDTO.SocialCompleteDTO request = new UserRequestDTO.SocialCompleteDTO(
                        "social_nick", 1, 1L, List.of("STUDY"), List.of("DESIGN")
                );

                Users mockUser = Users.builder().id(2L).status(UserStatus.ACTIVE).build();
                mockUser.setCreatedAt(LocalDateTime.now());

                given(userService.socialCompleteProfile(any(), any())).willReturn(mockUser);

                // when & then
                mockMvc.perform(patch("/api/v1/users/social/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andExpect(jsonPath("$.code").value("USERS2004"))
                        .andExpect(jsonPath("$.result.userId").value(2L))
                        .andDo(document("user/social-complete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("nickName").description("닉네임 (중복 불가)"),
                                        fieldWithPath("gender").description("성별 (1:남, 2:여)"),
                                        fieldWithPath("jobId").description("직업 ID"),
                                        fieldWithPath("purposeList").description("사용 목적 리스트"),
                                        fieldWithPath("interestList").description("관심사 리스트")
                                ),
                                responseFields(
                                        fieldWithPath("isSuccess").description("성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("timestamp").description("응답 시간"),
                                        fieldWithPath("result.userId").description("유저 ID"),
                                        fieldWithPath("result.createdAt").description("수정 일시")
                                )
                        ));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("실패 - 이미 프로필 완성이 끝난 사용자면 예외 응답을 반환한다")
            @WithCustomUser(userId = 2L)
            void social_complete_fail_already_active() throws Exception {
                // given
                UserRequestDTO.SocialCompleteDTO request = new UserRequestDTO.SocialCompleteDTO(
                        "already_active", 1, 1L, List.of("STUDY"), List.of("DESIGN")
                );

                given(userService.socialCompleteProfile(any(), any()))
                        .willThrow(new com.umc.linkyou.apiPayload.exception.handler.UserHandler(ErrorStatus._ALREADY_ACTIVE_USER));

                // when & then
                mockMvc.perform(patch("/api/v1/users/social/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.isSuccess").value(false))
                        .andExpect(jsonPath("$.code").value(ErrorStatus._ALREADY_ACTIVE_USER.getCode()))
                        .andDo(document("user/social-complete-fail",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("isSuccess").description("성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("timestamp").description("응답 시간")
                                )
                        ));
            }
        }
    }

    @Nested
    @DisplayName("회원 탈퇴 엔드포인트")
    class Withdraw {

        @Test
        @DisplayName("성공 - 탈퇴 사유를 받아 사용자를 비활성화한다")
        @WithCustomUser(userId = 49L)
        void withdraw_me_success() throws Exception {
            // given
            UserRequestDTO.DeleteReasonDTO request = new UserRequestDTO.DeleteReasonDTO();
            request.setReason("서비스가 마음에 안 들어요.");

            Users mockUser = Users.builder()
                    .id(49L)
                    .status(UserStatus.INACTIVE)
                    .build();
            mockUser.setCreatedAt(LocalDateTime.now());

            given(userWithdrawService.withdrawUser(any(), any())).willReturn(mockUser);

            // when & then
            mockMvc.perform(post("/api/v1/users/inactive")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andExpect(jsonPath("$.code").value("USERS2003"))
                    .andExpect(jsonPath("$.result.userId").value(49L))
                    .andDo(document("user/withdraw",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("reason").description("탈퇴 사유")
                            ),
                            responseFields(
                                    fieldWithPath("isSuccess").description("성공 여부"),
                                    fieldWithPath("code").description("응답 코드"),
                                    fieldWithPath("message").description("응답 메시지"),
                                    fieldWithPath("timestamp").description("응답 시간"),
                                    fieldWithPath("result.userId").description("탈퇴 처리된 유저 ID"),
                                    fieldWithPath("result.nickname").description("유저 닉네임 (탈퇴 시 null 가능)").optional(),
                                    fieldWithPath("result.status").description("변경 된 상태 (INACTIVE)"),
                                    fieldWithPath("result.createdAt").description("생성 일시"),
                                    fieldWithPath("result.inactiveDate").description("탈퇴 처리 일시").optional()
                            )
                    ));
        }
    }
}
