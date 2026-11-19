package com.umc.linkyou.service.users;

import org.springframework.stereotype.Service;

import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.jwt.TokenIssueService;
import com.umc.linkyou.web.dto.user.UserResponseDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserLoginService {

    private final UserStatusValidator userStatusValidator;
    private final TokenIssueService tokenIssueService;

    /**
     * 공통 소셜 로그인 처리
     *
     * @param user
     * @param resolvedEmail
     * @param provider
     * @param deviceId
     * @param deviceType
     * @return
     */
    public UserResponseDTO.LoginResultDTO handleSocialLogin(
            Users user,
            String resolvedEmail,
            Provider provider,
            String deviceId,
            DeviceType deviceType) {
        if (userStatusValidator.isWithinWithdrawGracePeriod(user)) {
            // 탈퇴 유예중인 사용자는 복구 토큰만 발급하고, refresh token은 발급하지 않음
            String recoveryToken =
                    tokenIssueService.issueRecoveryToken(
                            user.getId(), resolvedEmail, provider.name(), user.getRole());
            return UserConverter.toLoginResultDTO(user, recoveryToken, null);
        }
        userStatusValidator.validateLoginAllowed(user);

        TokenIssueService.IssuedTokenPair tokenPair =
                tokenIssueService.issueForStatus(
                        user.getId(),
                        resolvedEmail,
                        provider.name(),
                        user.getRole(),
                        user.getStatus(),
                        deviceId,
                        deviceType);

        return UserConverter.toLoginResultDTO(
                user, tokenPair.accessToken(), tokenPair.refreshToken());
    }
}
