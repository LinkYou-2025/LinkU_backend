package com.umc.linkyou.web.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.config.common.WebConfig;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.TermsType;
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
import com.umc.linkyou.web.dto.UserRequestDTO;
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
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension.class)
@Import({WebConfig.class, CurrentUserArgumentResolver.class, TestSecurityConfig.class})
class AuthControllerTest {

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
    @DisplayName("회원가입 엔드포인트")
    class Join {

        @Test
        @DisplayName("성공 - 회원가입 요청을 처리하고 생성 결과를 반환한다")
        void join_user_docs() throws Exception {
            UserRequestDTO.JoinDTO request = new UserRequestDTO.JoinDTO(
                    "링큐유저",
                    "test@example.com",
                    "password123",
                    1,
                    1L,
                    List.of("CAREER"),
                    List.of("IT"),
                    Map.of(
                            TermsType.PRIVACY_POLICY, true,
                            TermsType.TERMS_OF_USE, true
                    )
            );

            Users mockUser = Users.builder()
                    .id(1L)
                    .build();
            mockUser.setCreatedAt(LocalDateTime.now());

            given(userService.joinUser(any())).willReturn(mockUser);

            mockMvc.perform(post("/api/v1/auth/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isSuccess").value(true))
                    .andDo(document("user/join",
                            preprocessRequest(prettyPrint()),
                            preprocessResponse(prettyPrint()),
                            requestFields(
                                    fieldWithPath("nickName").description("닉네임"),
                                    fieldWithPath("email").description("이메일"),
                                    fieldWithPath("password").description("비밀번호"),
                                    fieldWithPath("gender").description("성별 (1:남, 2:여)"),
                                    fieldWithPath("jobId").description("직업 ID"),
                                    fieldWithPath("purposeList").description("가입 목적 리스트"),
                                    fieldWithPath("interestList").description("관심사 리스트"),
                                    fieldWithPath("termsMap").type(JsonFieldType.OBJECT).description("약관 동의 맵"),
                                    fieldWithPath("termsMap.*").type(JsonFieldType.BOOLEAN).description("각 약관별 동의 여부")
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
}
