package com.umc.linkyou.oauth2.mobile.service;

import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.web.dto.user.UserResponseDTO;

public interface MobileAuthService {
    UserResponseDTO.LoginResultDTO login(String token, String deviceId, DeviceType deviceType);
}
