package com.umc.linkyou.oauth2.mobile.service;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.oauth2.utils.UserSocialLoginHelper;
import com.umc.linkyou.oauth2.mobile.client.NaverTokenClient;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.service.users.UserLoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NaverMobileAuthService implements MobileAuthService {

    private final NaverTokenClient naverTokenClient;
    private final UserSocialLoginHelper userSocialLoginHelper;
    private final UserLoginService userLoginService;
    private final AuthAccountRepository authAccountRepository;

    @Override
    public MobileLoginResponse login(String accessToken, String deviceId, DeviceType deviceType) {
        NaverTokenClient.NaverUserInfo info = naverTokenClient.getUserInfo(accessToken);

        Users user = userSocialLoginHelper.findOrCreateUser(
                info.email(), info.name(), info.externalId(),
                info.profileImage(), Provider.NAVER);

        String resolvedEmail = authAccountRepository.findByUserIdAndProvider(user.getId(), Provider.NAVER)
                .map(AuthAccount::getEmail)
                .orElse(info.email());
        return userLoginService.handleSocialLogin(user, resolvedEmail, Provider.NAVER, deviceId, deviceType);
    }

}
