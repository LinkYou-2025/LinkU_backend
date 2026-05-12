package com.umc.linkyou.oauth2.mobile.service;


import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.config.security.jwt.RefreshTokenManager;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth2.UserSocialLoginHelper;
import com.umc.linkyou.oauth2.mobile.client.GoogleTokenVerifier;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
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
    private final AuthAccountRepository authAccountRepository;

    @Value("${jwt.token.expiration.refresh}")
    private long refreshTtlMs;

    @Override
    public MobileLoginResponse login(String idToken) {
        GoogleTokenVerifier.GoogleUserInfo info = googleTokenVerifier.verify(idToken);
        String email = info.email();
        Users user = userSocialLoginHelper.findOrCreateUser(
                info.email(), info.name(), info.externalId(),
                info.profileImage(), Provider.GOOGLE);
        String resolvedEmail = authAccountRepository.findByUserIdAndProvider(user.getId(), Provider.GOOGLE)
                .map(AuthAccount::getEmail)
                .orElse(email);
        if (user.getStatus() == UserStatus.TEMP) {
            return MobileLoginResponse.builder()
                    .userId(user.getId())
                    .accessToken(jwtTokenProvider.createAccessToken(resolvedEmail, Provider.GOOGLE.name(),user.getRole()))
                    .refreshToken(null)
                    .status(UserStatus.TEMP)
                    .build();
        }

        String accessToken = jwtTokenProvider.createAccessToken(resolvedEmail, Provider.GOOGLE.name(),user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(resolvedEmail, Provider.GOOGLE.name());  //
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
