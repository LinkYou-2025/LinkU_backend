package com.umc.linkyou.web.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.domain.Users;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
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

    @MockitoBean
    private SecurityErrorResponseWriter securityErrorResponseWriter;

    // ────────────────────────────────────────────────────────────────
    // 소셜 프로필 완성 엔드포인트
    // ────────────────────────────────────────────────────────────────
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
                        1,
                        1L,
                        List.of("STUDY"),
                        List.of("DESIGN"),
                        Map.of(TermsType.TERMS_OF_USE, true)
                );

                Users mockUser = Users.builder()
                        .id(2L)
                        .status(UserStatus.ACTIVE)
                        .build();
                mockUser.setCreatedAt(LocalDateTime.now());

                given(userService.socialCompleteProfile(any(), any())).willReturn(mockUser);

                mockMvc.perform(patch("/api/v1/users/social/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andDo(document("user/social-complete",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("nickName").description("닉네임"),
                                        fieldWithPath("gender").description("성별"),
                                        fieldWithPath("jobId").description("직업 ID"),
                                        fieldWithPath("purposeList").description("사용 목적"),
                                        fieldWithPath("interestList").description("관심사"),
                                        fieldWithPath("termsMap").type(JsonFieldType.OBJECT).description("약관 동의 맵"),
                                        fieldWithPath("termsMap.*").type(JsonFieldType.BOOLEAN).description("각 약관별 동의 여부")
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
            @DisplayName("실패 - 비인증 사용자가 요청 시 401 에러를 반환한다")
            void social_complete_unauthorized() throws Exception {
                UserRequestDTO.SocialCompleteDTO request = new UserRequestDTO.SocialCompleteDTO(
                        "social_nick",
                        1,
                        1L,
                        List.of("STUDY"),
                        List.of("DESIGN"),
                        Map.of(TermsType.MARKETING, true)
                );

                mockMvc.perform(patch("/api/v1/users/social/complete")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isUnauthorized());
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // 약관 상태 조회 엔드포인트
    // ────────────────────────────────────────────────────────────────
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
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andDo(document("user/terms-status",
                                preprocessResponse(prettyPrint()),
                                responseFields(
                                        fieldWithPath("isSuccess").description("성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("timestamp").description("응답 시간"),
                                        fieldWithPath("result.userId").description("유저 ID"),
                                        fieldWithPath("result.termsStatus").type(JsonFieldType.OBJECT).description("약관별 동의 여부 맵"),
                                        fieldWithPath("result.termsStatus.*").type(JsonFieldType.BOOLEAN).description("각 약관 타입별 동의 여부"),
                                        fieldWithPath("result.allRequiredAgreed").description("필수 약관 전체 동의 여부")
                                )
                        ));
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

    // ────────────────────────────────────────────────────────────────
    // 약관 일괄 업데이트 엔드포인트
    // ────────────────────────────────────────────────────────────────
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
                        .andExpect(jsonPath("$.isSuccess").value(true))
                        .andDo(document("user/terms-update",
                                preprocessRequest(prettyPrint()),
                                preprocessResponse(prettyPrint()),
                                requestFields(
                                        fieldWithPath("termsMap").type(JsonFieldType.OBJECT).description("변경할 약관 타입 및 동의 여부 맵"),
                                        fieldWithPath("termsMap.*").type(JsonFieldType.BOOLEAN).description("각 약관 타입별 동의 여부"),
                                        fieldWithPath("termsVersion").description("약관 버전")
                                ),
                                responseFields(
                                        fieldWithPath("isSuccess").description("성공 여부"),
                                        fieldWithPath("code").description("응답 코드"),
                                        fieldWithPath("message").description("응답 메시지"),
                                        fieldWithPath("timestamp").description("응답 시간"),
                                        fieldWithPath("result.userId").description("유저 ID"),
                                        fieldWithPath("result.termsStatus").type(JsonFieldType.OBJECT).description("업데이트된 약관 동의 상태"),
                                        fieldWithPath("result.termsStatus.*").type(JsonFieldType.BOOLEAN).description("각 약관 타입별 업데이트된 동의 여부"),
                                        fieldWithPath("result.allRequiredAgreed").description("필수 약관 전체 동의 여부")
                                )
                        ));
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("실패 - 유효하지 않은 약관 타입이 포함된 경우 Jackson 역직렬화 시점에 400을 반환한다")
            @WithCustomUser(userId = 1L)
            void update_terms_fail_invalid_type() throws Exception {
                // [변경] Map<TermsType, Boolean>으로 바뀌었으므로
                // 컴파일 타임에 "INVALID_TYPE" 같은 잘못된 키를 넣을 수 없음
                // → raw JSON 문자열을 직접 작성해서 Jackson 역직렬화 실패를 유도
                String invalidJson = """
                        {
                          "termsMap": { "INVALID_TYPE": true },
                          "termsVersion": "v1.0"
                        }
                        """;

                // [변경] given() 제거 — 서비스까지 도달하지 않고 Jackson이 400 반환
                mockMvc.perform(patch("/api/v1/users/terms/agree")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson)
                                .with(csrf()))
                        .andExpect(status().isBadRequest());
            }

            @Test
            @DisplayName("실패 - 비인증 사용자가 요청 시 401 에러를 반환한다")
            void update_terms_unauthorized() throws Exception {
                // [변경] Map.of("MARKETING", true) → Map.of(TermsType.MARKETING, true)
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
}
