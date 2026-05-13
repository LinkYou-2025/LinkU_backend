package com.umc.linkyou.oauth2.mobile.service;

import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;

public interface MobileAuthService {
    MobileLoginResponse login(String token, String deviceId, DeviceType deviceType);
}
