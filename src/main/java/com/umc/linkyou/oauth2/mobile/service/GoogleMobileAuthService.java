package com.umc.linkyou.oauth2.mobile.service;


import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.config.security.jwt.RefreshTokenManager;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth2.UserSocialLoginHelper;
import com.umc.linkyou.oauth2.mobile.client.GoogleTokenVerifier;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GoogleMobileAuthService implements MobileAuthService {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final UserSocialLoginHelper userSocialLoginHelper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenManager refreshTokenManager;

    @Value("${jwt.token.expiration.refresh}")
    private long refreshTtlMs;

    @Override
    public MobileLoginResponse login(String idToken) {
        GoogleTokenVerifier.GoogleUserInfo info = googleTokenVerifier.verify(idToken);
        String email = info.email();
        Users user = userSocialLoginHelper.findOrCreateUser(
                info.email(), info.name(), info.externalId(),
                info.profileImage(), Provider.GOOGLE);

        if (user.getStatus() == UserStatus.TEMP) {
            return MobileLoginResponse.builder()
                    .userId(user.getId())
                    .accessToken(jwtTokenProvider.createAccessToken(user.getEmail(), Provider.GOOGLE.name()))
                    .refreshToken(null)
                    .status(UserStatus.TEMP)
                    .build();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), Provider.GOOGLE.name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());  //
        String tokenId = jwtTokenProvider.hmac(jwtTokenProvider.normalizeStrict(refreshToken));
        refreshTokenManager.saveToken(user.getId(), tokenId, Provider.GOOGLE.name(), refreshTtlMs);

        return MobileLoginResponse.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
