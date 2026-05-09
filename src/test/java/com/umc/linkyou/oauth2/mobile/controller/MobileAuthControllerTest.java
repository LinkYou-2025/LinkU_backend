package com.umc.linkyou.oauth2.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginRequest;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import com.umc.linkyou.oauth2.mobile.service.GoogleMobileAuthService;
import com.umc.linkyou.oauth2.mobile.service.KakaoMobileAuthService;
import com.umc.linkyou.oauth2.mobile.service.NaverMobileAuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(MobileAuthController.class)
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension.class)
class MobileAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccessTokenBlackListManager accessTokenBlackListManager;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GoogleMobileAuthService googleService;

    @MockitoBean
    private KakaoMobileAuthService kakaoService;

    @MockitoBean
    private NaverMobileAuthService naverService;

    @Test
    @DisplayName("카카오 로그인 API 문서화")
    void kakao_login_docs() throws Exception {
        // given
        MobileLoginRequest request = new MobileLoginRequest("kakao_access_token_123", "ios-iphone-16-pro", DeviceType.PHONE);
        MobileLoginResponse response = MobileLoginResponse.builder()
                .userId(1L)
                .accessToken("jwt_access_token")
                .refreshToken("jwt_refresh_token")
                .status(UserStatus.ACTIVE)
                .build();

        given(kakaoService.login(any(), any(), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/auth/mobile/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf())
                        .with(user("test")))
                .andExpect(status().isOk())
                .andDo(document("auth/mobile/kakao",
                        preprocessRequest(prettyPrint()),
                        preprocessResponse(prettyPrint()),
                        requestFields(
                                fieldWithPath("token").description("카카오 SDK에서 받은 accessToken"),
                                fieldWithPath("deviceId").description("클라이언트 기기 식별자"),
                                fieldWithPath("deviceType").description("기기 종류")
                        ),
                        responseFields(
                                fieldWithPath("isSuccess").description("성공 여부"),
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("timestamp").description("응답 시간"),
                                fieldWithPath("result.userId").description("유저 ID"),
                                fieldWithPath("result.accessToken").description("JWT 액세스 토큰"),
                                fieldWithPath("result.refreshToken").description("JWT 리프레시 토큰 (TEMP 유저는 null)").optional(),
                                fieldWithPath("result.status").description("유저 상태 (ACTIVE 또는 TEMP)")
                        )
                ));
    }
}
