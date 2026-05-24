package com.umc.linkyou.oauth2.mobile.dto;

import com.umc.linkyou.domain.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MobileLoginResponse {
    private Long userId;
    private String accessToken;
    private String refreshToken;   // TEMP 유저는 null
    private UserStatus status;     // ACTIVE or TEMP
}