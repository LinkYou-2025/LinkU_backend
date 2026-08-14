package com.umc.linkyou.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.linkyou.config.properties.JwtProperties;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Role;

@ExtendWith(MockitoExtension.class)
class TokenIssueServiceTest {

    @InjectMocks private TokenIssueService tokenIssueService;

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenManager refreshTokenManager;
    @Mock private JwtProperties jwtProperties;
    @Mock private JwtProperties.Expiration expiration;

    @Nested
    @DisplayName("issueTokenPair")
    class IssueTokenPair {

        @Test
        @DisplayName("새 sid를 액세스 토큰과 리프레시 세션에 함께 사용한다")
        void 새_sid를_액세스_토큰과_리프레시_세션에_함께_사용한다() {
            // given
            long accessExpiresAt = System.currentTimeMillis() + 3_600_000L;
            when(jwtTokenProvider.createAccessToken(
                            eq(1L), eq("user@test.com"), eq("KAKAO"), eq(Role.USER), anyString()))
                    .thenReturn("access-token");
            when(jwtTokenProvider.createRefreshToken("user@test.com", "KAKAO"))
                    .thenReturn("refresh-token");
            when(jwtTokenProvider.normalizeStrict("refresh-token")).thenReturn("refresh-token");
            when(jwtTokenProvider.hmac("refresh-token")).thenReturn("refresh-token-id");
            when(jwtTokenProvider.getAccessTokenExpiresAt("access-token")).thenReturn(accessExpiresAt);
            when(jwtProperties.expiration()).thenReturn(expiration);
            when(expiration.refresh()).thenReturn(1_209_600_000L);
            when(refreshTokenManager.saveToken(
                            eq(1L),
                            eq("KAKAO"),
                            eq("device-1"),
                            eq("refresh-token-id"),
                            eq(DeviceType.PHONE),
                            anyLong(),
                            anyString(),
                            eq(accessExpiresAt)))
                    .thenReturn(Optional.empty());

            // when
            TokenIssueService.IssuedTokenPair result =
                    tokenIssueService.issueTokenPair(
                            1L,
                            "user@test.com",
                            "KAKAO",
                            Role.USER,
                            "device-1",
                            DeviceType.PHONE);

            // then
            assertEquals("access-token", result.accessToken());
            assertEquals("refresh-token", result.refreshToken());

            ArgumentCaptor<String> issuedSessionIdCaptor = ArgumentCaptor.forClass(String.class);
            verify(jwtTokenProvider)
                    .createAccessToken(
                            eq(1L),
                            eq("user@test.com"),
                            eq("KAKAO"),
                            eq(Role.USER),
                            issuedSessionIdCaptor.capture());
            verify(refreshTokenManager)
                    .saveToken(
                            eq(1L),
                            eq("KAKAO"),
                            eq("device-1"),
                            eq("refresh-token-id"),
                            eq(DeviceType.PHONE),
                            anyLong(),
                            eq(issuedSessionIdCaptor.getValue()),
                            eq(accessExpiresAt));
        }
    }
}
