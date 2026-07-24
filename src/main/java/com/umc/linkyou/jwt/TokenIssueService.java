package com.umc.linkyou.jwt;

import com.umc.linkyou.config.properties.JwtProperties;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.enums.UserStatus;
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

    // 액세스 토큰 발급 (리프레시 토큰은 DB에 저장되어야 하므로, 별도 메서드에서 발급)
    public String issueAccessToken(Long userId, String email, String provider, Role role) {
        return jwtTokenProvider.createAccessToken(userId, email, provider, role);
    }

    // 탈퇴 유예 기간 복구 전용 토큰, TTL 10분으로 일반 로그인에는 사용 불가능
    public String issueRecoveryToken(Long userId, String email, String provider, Role role) {
        return jwtTokenProvider.createRecoveryToken(userId, email, provider, role);
    }

    // 리프레시 토큰은 DB에 저장되어야 하므로, 발급 시 DB에 저장하는 로직을 포함
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
        long expiresAt = System.currentTimeMillis() + jwtProperties.expiration().refresh();

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

    /**
     * 유저 상태에 따라 발급 범위를 결정
     * TEMP(온보딩 미완료): 세션이 오래 유지되지 않도록 access token만 발급
     * 그 외의 경우 : access + refresh 토큰 쌍 발급
     */
    public IssuedTokenPair issueForStatus(
            Long userId,
            String email,
            String provider,
            Role role,
            UserStatus status,
            String deviceId,
            DeviceType deviceType
    ) {
        if (status == UserStatus.TEMP) {
            return new IssuedTokenPair(issueAccessToken(userId, email, provider, role), null);
        }
        return issueTokenPair(userId, email, provider, role, deviceId, deviceType);
    }

    public record IssuedTokenPair(String accessToken, String refreshToken) {
    }
}
