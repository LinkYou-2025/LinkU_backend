package com.umc.linkyou.service.users;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.jwt.TokenIssueService;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserLoginService {

    private final UserStatusValidator userStatusValidator;
    private final TokenIssueService tokenIssueService;

    /**
     * 공통 소셜 로그인 처리
     * @param user
     * @param resolvedEmail
     * @param provider
     * @param deviceId
     * @param deviceType
     * @return
     */
    public MobileLoginResponse handleSocialLogin(
            Users user,
            String resolvedEmail,
            Provider provider,
            String deviceId,
            DeviceType deviceType
    ) {
        userStatusValidator.validateLoginAllowed(user);

        TokenIssueService.IssuedTokenPair tokenPair =
                tokenIssueService.issueForStatus(
                        user.getId(),
                        resolvedEmail,
                        provider.name(),
                        user.getRole(),
                        user.getStatus(),
                        deviceId,
                        deviceType
                );

        return MobileLoginResponse.builder()
                .userId(user.getId())
                .accessToken(tokenPair.accessToken())
                .refreshToken(tokenPair.refreshToken())
                .status(user.getStatus())
                .build();
    }
}
