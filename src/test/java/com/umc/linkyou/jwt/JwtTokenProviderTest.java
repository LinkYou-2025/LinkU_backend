package com.umc.linkyou.jwt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.umc.linkyou.config.properties.JwtProperties;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;

class JwtTokenProviderTest {

    private final JwtTokenProvider jwtTokenProvider = createJwtTokenProvider();

    @Nested
    @DisplayName("createAccessToken")
    class CreateAccessToken {

        @Test
        @DisplayName("세션 ID를 전달하면 액세스 토큰에서 같은 sid를 추출한다")
        void 세션_ID를_전달하면_액세스_토큰에서_같은_sid를_추출한다() {
            // given
            String sessionId = "session-id-1";

            // when
            String accessToken =
                    jwtTokenProvider.createAccessToken(
                            1L, "user@test.com", "KAKAO", Role.USER, sessionId);

            // then
            assertEquals(sessionId, jwtTokenProvider.getSessionId(accessToken));
            assertTrue(jwtTokenProvider.getAccessTokenExpiresAt(accessToken) > System.currentTimeMillis());
        }
    }

    private JwtTokenProvider createJwtTokenProvider() {
        JwtProperties jwtProperties =
                new JwtProperties(
                        new JwtProperties.Keys(
                                "test-access-signing-key-must-be-at-least-32-bytes",
                                "test-refresh-signing-key-must-be-at-least-32-bytes"),
                        "test-issuer",
                        new JwtProperties.Expiration(3_600_000L, 1_209_600_000L));
        JwtTokenProvider provider =
                new JwtTokenProvider(
                        jwtProperties,
                        mock(AuthAccountRepository.class),
                        mock(RefreshTokenManager.class));
        provider.init();
        return provider;
    }
}
