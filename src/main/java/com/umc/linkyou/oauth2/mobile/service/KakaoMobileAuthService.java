package com.umc.linkyou.oauth2.mobile.service;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.oauth2.utils.UserSocialLoginHelper;
import com.umc.linkyou.oauth2.mobile.client.KakaoTokenClient;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.service.users.UserLoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class KakaoMobileAuthService implements MobileAuthService {

    private final KakaoTokenClient kakaoTokenClient;
    private final UserSocialLoginHelper userSocialLoginHelper;
    private final UserLoginService userLoginService;
    private final AuthAccountRepository authAccountRepository;

    @Override
    public MobileLoginResponse login(String accessToken, String deviceId, DeviceType deviceType) {
        KakaoTokenClient.KakaoUserInfo info = kakaoTokenClient.getUserInfo(accessToken);

        Users user = userSocialLoginHelper.findOrCreateUser(
                info.email(), info.name(), info.externalId(),
                info.profileImage(), Provider.KAKAO);

        String resolvedEmail = authAccountRepository.findByUserIdAndProvider(user.getId(), Provider.KAKAO)
                .map(AuthAccount::getEmail)
                .orElse(info.email());
        return userLoginService.handleSocialLogin(user, resolvedEmail, Provider.KAKAO, deviceId, deviceType);
    }

}
