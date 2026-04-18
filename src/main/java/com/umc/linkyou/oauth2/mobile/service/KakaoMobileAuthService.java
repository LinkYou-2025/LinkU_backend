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
import com.umc.linkyou.oauth2.mobile.client.KakaoTokenClient;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KakaoMobileAuthService implements MobileAuthService {

    private final KakaoTokenClient kakaoTokenClient;
    private final UserSocialLoginHelper userSocialLoginHelper;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenManager refreshTokenManager;
    private final AuthAccountRepository authAccountRepository;

    @Value("${jwt.token.expiration.refresh}")
    private long refreshTtlMs;

    @Override
    public MobileLoginResponse login(String accessToken) {
        KakaoTokenClient.KakaoUserInfo info = kakaoTokenClient.getUserInfo(accessToken);

        Users user = userSocialLoginHelper.findOrCreateUser(
                info.email(), info.name(), info.externalId(),
                info.profileImage(), Provider.KAKAO);

        String resolvedEmail = authAccountRepository.findByUserIdAndProvider(user.getId(), Provider.KAKAO)
                .map(AuthAccount::getEmail)
                .orElse(info.email());

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new GeneralException(ErrorStatus._USER_INACTIVE);
        }

        if (user.getStatus() == UserStatus.TEMP) {
            return MobileLoginResponse.builder()
                    .userId(user.getId())
                    .accessToken(jwtTokenProvider.createAccessToken(resolvedEmail, Provider.KAKAO.name(),user.getRole()))
                    .refreshToken(null)
                    .status(UserStatus.TEMP)
                    .build();
        }

        // ACTIVE 사용자만 풀 토큰 발급
        String accessTokenJwt = jwtTokenProvider.createAccessToken(resolvedEmail, Provider.KAKAO.name(),user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(resolvedEmail, Provider.KAKAO.name());
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
