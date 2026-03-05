package com.umc.linkyou.oauth2.mobile.service;

import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.config.security.jwt.RefreshTokenManager;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth2.UserSocialLoginHelper;
import com.umc.linkyou.oauth2.mobile.client.KakaoTokenClient;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class KakaoMobileAuthService implements MobileAuthService {

    private final KakaoTokenClient kakaoTokenClient;
    private final UserSocialLoginHelper userSocialLoginHelper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenManager refreshTokenManager;

    @Value("${jwt.token.expiration.refresh}")
    private long refreshTtlMs;

    @Override
    public MobileLoginResponse login(String accessToken) {  // idToken → accessToken
        KakaoTokenClient.KakaoUserInfo info = kakaoTokenClient.getUserInfo(accessToken);

        Users user = userSocialLoginHelper.findOrCreateUser(
                info.email(), info.name(), info.externalId(),
                info.profileImage(), Provider.KAKAO);  // Provider.KAKAO

        if (user.getStatus() == UserStatus.TEMP) {
            return MobileLoginResponse.builder()
                    .userId(user.getId())
                    .accessToken(jwtTokenProvider.createAccessToken(info.email(), Provider.KAKAO.name()))
                    .refreshToken(null)
                    .status(UserStatus.TEMP)
                    .build();
        }

        String accessTokenJwt = jwtTokenProvider.createAccessToken(info.email(), Provider.KAKAO.name());
        String refreshToken = jwtTokenProvider.createRefreshToken(info.email());
        String tokenId = jwtTokenProvider.hmac(jwtTokenProvider.normalizeStrict(refreshToken));
        refreshTokenManager.saveToken(user.getId(), tokenId, Provider.KAKAO.name(), refreshTtlMs);

        return MobileLoginResponse.builder()
                .userId(user.getId())
                .accessToken(accessTokenJwt)
                .refreshToken(refreshToken)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
