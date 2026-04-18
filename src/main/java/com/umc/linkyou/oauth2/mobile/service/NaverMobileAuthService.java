package com.umc.linkyou.oauth2.mobile.service;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.config.security.jwt.RefreshTokenManager;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth2.UserSocialLoginHelper;
import com.umc.linkyou.oauth2.mobile.client.NaverTokenClient;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NaverMobileAuthService implements MobileAuthService {

    private final NaverTokenClient naverTokenClient;
    private final UserSocialLoginHelper userSocialLoginHelper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenManager refreshTokenManager;
    private final AuthAccountRepository authAccountRepository;

    @Value("${jwt.token.expiration.refresh}")
    private long refreshTtlMs;

    @Override
    public MobileLoginResponse login(String accessToken) {
        NaverTokenClient.NaverUserInfo info = naverTokenClient.getUserInfo(accessToken);

        Users user = userSocialLoginHelper.findOrCreateUser(
                info.email(), info.name(), info.externalId(),
                info.profileImage(), Provider.NAVER);

        String resolvedEmail = authAccountRepository.findByUserIdAndProvider(user.getId(), Provider.NAVER)
                .map(AuthAccount::getEmail)
                .orElse(info.email());

        if (user.getStatus() != UserStatus.ACTIVE && user.getStatus() != UserStatus.TEMP) {
            throw new GeneralException(ErrorStatus._USER_INACTIVE);
        }

        String accessTokenJwt = jwtTokenProvider.createAccessToken(resolvedEmail, Provider.NAVER.name(),user.getRole());

        if (user.getStatus() == UserStatus.TEMP) {
            return MobileLoginResponse.builder()
                    .userId(user.getId())
                    .accessToken(accessTokenJwt)
                    .refreshToken(null)
                    .status(UserStatus.TEMP)
                    .build();
        }

        // ACTIVE 전용: refreshToken + Redis 세션
        String refreshToken = jwtTokenProvider.createRefreshToken(resolvedEmail, Provider.NAVER.name());
        String tokenId = jwtTokenProvider.hmac(jwtTokenProvider.normalizeStrict(refreshToken));
        refreshTokenManager.saveToken(user.getId(), tokenId, Provider.NAVER.name(), refreshTtlMs);

        return MobileLoginResponse.builder()
                .userId(user.getId())
                .accessToken(accessTokenJwt)
                .refreshToken(refreshToken)
                .status(UserStatus.ACTIVE)
                .build();
    }

}
