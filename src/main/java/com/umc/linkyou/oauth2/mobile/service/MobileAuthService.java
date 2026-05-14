package com.umc.linkyou.oauth2.mobile.service;

import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;

public interface MobileAuthService {
    MobileLoginResponse login(String token);
}
