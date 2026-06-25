package com.umc.linkyou.oauth2.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.SecurityErrorResponseWriter;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginRequest;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import com.umc.linkyou.oauth2.mobile.service.GoogleMobileAuthService;
import com.umc.linkyou.oauth2.mobile.service.KakaoMobileAuthService;
import com.umc.linkyou.oauth2.mobile.service.NaverMobileAuthService;
import com.umc.linkyou.support.security.TestSecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MobileAuthController.class)
@Import(TestSecurityConfig.class)
class MobileAuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private AccessTokenBlackListManager accessTokenBlackListManager;
    @MockitoBean private SecurityErrorResponseWriter securityErrorResponseWriter;
    @MockitoBean private GoogleMobileAuthService googleService;
    @MockitoBean private KakaoMobileAuthService kakaoService;
    @MockitoBean private NaverMobileAuthService naverService;

    @Nested
    @DisplayName("카카오 모바일 로그인")
    class KakaoMobileLogin {

        private MobileLoginRequest createRequest(String token, String deviceId, DeviceType deviceType) {
            return new MobileLoginRequest(token, deviceId, deviceType);
        }

        @Nested
        @DisplayName("성공 케이스")
        class Success {

            @Test
            @DisplayName("기존 회원 카카오 로그인")
            void kakao_login_existing_user() throws Exception {
                MobileLoginRequest request = createRequest(
                        "kakao_access_token_123", "ios-iphone-16-pro", DeviceType.PHONE);

                MobileLoginResponse response = MobileLoginResponse.builder()
                        .userId(1L)
                        .accessToken("jwt_access_token")
                        .refreshToken("jwt_refresh_token")
                        .status(UserStatus.ACTIVE)
                        .build();

                given(kakaoService.login(any(), any(), any())).willReturn(response);

                mockMvc.perform(post("/api/v1/auth/mobile/kakao")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.isSuccess").value(true));
            }

            @Test
            @DisplayName("TEMP 유저 카카오 로그인")
            void kakao_login_temp_user() throws Exception {
                MobileLoginRequest request = createRequest(
                        "kakao_temp_token_456", "android-galaxy-s24", DeviceType.PHONE);

                MobileLoginResponse response = MobileLoginResponse.builder()
                        .userId(999L)
                        .accessToken("jwt_temp_access_token")
                        .refreshToken(null)
                        .status(UserStatus.TEMP)
                        .build();

                given(kakaoService.login(any(), any(), any())).willReturn(response);

                mockMvc.perform(post("/api/v1/auth/mobile/kakao")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isOk());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class Failure {

            @Test
            @DisplayName("유효하지 않은 카카오 토큰")
            void kakao_login_invalid_token() throws Exception {
                MobileLoginRequest request = createRequest(
                        "invalid_kakao_token", "ios-iphone-16-pro", DeviceType.PHONE);

                given(kakaoService.login(any(), any(), any()))
                        .willThrow(new RuntimeException("Invalid Kakao token"));

                mockMvc.perform(post("/api/v1/auth/mobile/kakao")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isInternalServerError());
            }

            @Test
            @DisplayName("기기 ID 누락")
            void kakao_login_missing_device_id() throws Exception {
                MobileLoginRequest request = createRequest(
                        "kakao_access_token_123", null, DeviceType.PHONE);

                mockMvc.perform(post("/api/v1/auth/mobile/kakao")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                                .with(csrf()))
                        .andExpect(status().isBadRequest());
            }
        }
    }

    @Nested
    @DisplayName("구글 모바일 로그인")
    class GoogleMobileLogin {

        @Test
        @DisplayName("구글 모바일 로그인 성공")
        void google_login_success() throws Exception {
            MobileLoginRequest request = new MobileLoginRequest(
                    "google_access_token_abc", "ios-ipad-pro", DeviceType.TABLET);

            MobileLoginResponse response = MobileLoginResponse.builder()
                    .userId(3L)
                    .accessToken("google_jwt_access_token")
                    .refreshToken("google_jwt_refresh_token")
                    .status(UserStatus.ACTIVE)
                    .build();

            given(googleService.login(any(), any(), any())).willReturn(response);

            mockMvc.perform(post("/api/v1/auth/mobile/google")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("네이버 모바일 로그인")
    class NaverMobileLogin {

        @Test
        @DisplayName("네이버 모바일 로그인 성공")
        void naver_login_success() throws Exception {
            MobileLoginRequest request = new MobileLoginRequest(
                    "naver_access_token_xyz", "android-pixel-8", DeviceType.PHONE);

            MobileLoginResponse response = MobileLoginResponse.builder()
                    .userId(5L)
                    .accessToken("naver_jwt_access_token")
                    .refreshToken("naver_jwt_refresh_token")
                    .status(UserStatus.ACTIVE)
                    .build();

            given(naverService.login(any(), any(), any())).willReturn(response);

            mockMvc.perform(post("/api/v1/auth/mobile/naver")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                            .with(csrf()))
                    .andExpect(status().isOk());
        }
    }
}
