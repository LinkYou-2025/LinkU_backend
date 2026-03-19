package com.umc.linkyou.web.controller.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.service.users.TermsAgreementService;
import com.umc.linkyou.service.users.UserService;
import com.umc.linkyou.service.users.UserWithdrawService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.web.dto.UserRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@WebMvcTest(UserController.class)
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension.class)
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
    private UsersUtils usersUtils;

    @MockitoBean
    private TermsAgreementService termsAgreementService;

    @Test
    @DisplayName("일반 회원가입 API 문서화")
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
        mockMvc.perform(post("/api/v1/users/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())               // 403 Forbidden 해결
                        .with(user("test")))        // 302 Redirect 해결 (기본 ROLE_USER 권한 부여됨)
                .andExpect(status().isOk())
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

    @Test
    @DisplayName("소셜 프로필 완성 API 문서화")
    void social_complete_docs() throws Exception {
        // given
        UserRequestDTO.SocialCompleteDTO request = new UserRequestDTO.SocialCompleteDTO(
                "social_nick", 1, 1L, List.of("STUDY"), List.of("DESIGN")
        );

        Users mockUser = Users.builder().id(2L).status(UserStatus.ACTIVE).build();
        mockUser.setCreatedAt(LocalDateTime.now());

        given(usersUtils.validateTempUser(any())).willReturn(mockUser);
        given(userService.socialCompleteProfile(any(), any())).willReturn(mockUser);

        // when & then
        mockMvc.perform(patch("/api/v1/users/social/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())               // 403 Forbidden 해결
                        .with(user("test")))        // 302 Redirect 해결 (기본 ROLE_USER 권한 부여됨)
                .andExpect(status().isOk())
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

    @Test
    @DisplayName("소셜 프로필 완성 실패 - 이미 완료된 유저")
    void social_complete_fail_already_active() throws Exception {
        // given
        UserRequestDTO.SocialCompleteDTO request = new UserRequestDTO.SocialCompleteDTO(
                "already_active", 1, 1L, List.of("STUDY"), List.of("DESIGN")
        );

        // 이미 ACTIVE 상태인 유저를 반환하도록 Mocking
        Users activeUser = Users.builder().id(2L).status(UserStatus.ACTIVE).build();

        // usersUtils.validateTempUser나 userService에서 예외를 던지도록 설정
        given(usersUtils.validateTempUser(any())).willReturn(activeUser);
        given(userService.socialCompleteProfile(any(), any()))
                .willThrow(new com.umc.linkyou.apiPayload.exception.handler.UserHandler(ErrorStatus._ALREADY_ACTIVE_USER));

        // when & then
        mockMvc.perform(patch("/api/v1/users/social/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(user("test")))
                .andExpect(status().isBadRequest()) // 400 에러 기대
                .andDo(document("user/social-complete-fail",
                        preprocessRequest(
                                prettyPrint()
                        ),
                        preprocessResponse(prettyPrint()),
                        responseFields(
                                fieldWithPath("isSuccess").description("성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시간")
                        )
                ));
    }

    @MockitoBean
    private UserWithdrawService userWithdrawService;

    @Test
    @DisplayName("회원 탈퇴 API 성공 테스트")
    void withdraw_me_success() throws Exception {
        // given
        UserRequestDTO.DeleteReasonDTO request = new UserRequestDTO.DeleteReasonDTO();
        request.setReason("서비스가 마음에 안 들어요.");

        Users mockUser = Users.builder()
                .id(49L)
                .status(UserStatus.INACTIVE)
                .build();
        mockUser.setCreatedAt(LocalDateTime.now());

        // Mocking: 서비스의 withdrawUser가 호출되면 mockUser를 반환하도록 설정
        given(usersUtils.getAuthenticatedUserId(any())).willReturn(49L);
        given(userWithdrawService.withdrawUser(any(), any())).willReturn(mockUser);

        // when & then
        mockMvc.perform(post("/api/v1/users/inactive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(user("test"))) // 시큐리티 인증 통과용
                .andExpect(status().isOk())
                .andDo(document("user/withdraw",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("reason").description("탈퇴 사유")
                        ),
                        // UserControllerTest.java의 responseFields 부분 수정
                        responseFields(
                                fieldWithPath("isSuccess").description("성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시간"),
                                fieldWithPath("result.userId").description("탈퇴 처리된 유저 ID"),
                                fieldWithPath("result.nickname").description("유저 닉네임 (탈퇴 시 null 가능)").optional(), // 추가
                                fieldWithPath("result.status").description("변경 된 상태 (INACTIVE)"),
                                fieldWithPath("result.createdAt").description("생성 일시"), // 필드명 확인 (응답 로그 기준)
                                fieldWithPath("result.inactiveDate").description("탈퇴 처리 일시").optional() // 추가
                        )
                ));
    }


}
