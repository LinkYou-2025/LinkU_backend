package com.umc.linkyou.oauth2.mobile.dto;

import com.umc.linkyou.domain.enums.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MobileLoginRequest(
        @Schema(description = "구글=ID Token / 카카오·네이버=accessToken")
        @NotBlank(message = "token은 필수입니다")
        String token,

        @Schema(description = "클라이언트가 생성/보관하는 기기 고유 식별자")
        @NotBlank(message = "deviceId는 필수입니다")
        String deviceId,

        @Schema(description = "기기 종류", example = "PHONE")
        @NotNull(message = "deviceType은 필수입니다")
        DeviceType deviceType
) {}
