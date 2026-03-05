package com.umc.linkyou.oauth2.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record MobileLoginRequest(
        @Schema(description = "구글=ID Token / 카카오·네이버=accessToken")
        @NotBlank(message = "token은 필수입니다")
        String token
) {}
