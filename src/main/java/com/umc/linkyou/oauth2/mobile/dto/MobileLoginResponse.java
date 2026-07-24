package com.umc.linkyou.oauth2.mobile.dto;

import com.umc.linkyou.domain.enums.UserStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MobileLoginResponse {
    private Long userId;
    private String accessToken;
    private String refreshToken;   // TEMP/탈퇴 유예(INACTIVE) 유저는 null
    private UserStatus status;     // ACTIVE, TEMP, INACTIVE(탈퇴 유예 - 복구 필요)
    private LocalDateTime inactiveDate; // INACTIVE(탈퇴 유예)일 때만 값이 있음
}