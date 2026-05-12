package com.umc.linkyou.jwt;

import com.umc.linkyou.config.properties.JwtProperties;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 액세스 토큰과 리프레시 토큰을 발급하는 서비스
 */
@Service
@RequiredArgsConstructor
public class TokenIssueService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenManager refreshTokenManager;
    private final JwtProperties jwtProperties;

    public String issueAccessToken(Long userId, String email, String provider, Role role) {
        return jwtTokenProvider.createAccessToken(userId, email, provider, role);
    }

    public IssuedTokenPair issueTokenPair(
            Long userId,
            String email,
            String provider,
            Role role,
            String deviceId,
            DeviceType deviceType
    ) {
        String accessToken = issueAccessToken(userId, email, provider, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(email, provider);
        String tokenId = jwtTokenProvider.hmac(jwtTokenProvider.normalizeStrict(refreshToken));
        long expiresAt = System.currentTimeMillis() + jwtProperties.getExpiration().getRefresh();

        refreshTokenManager.saveToken(
                userId,
                provider,
                deviceId,
                tokenId,
                deviceType,
                expiresAt
        );

        return new IssuedTokenPair(accessToken, refreshToken);
    }

    public record IssuedTokenPair(String accessToken, String refreshToken) {
    }
}
