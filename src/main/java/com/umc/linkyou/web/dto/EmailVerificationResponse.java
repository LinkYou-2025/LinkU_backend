package com.umc.linkyou.web.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class EmailVerificationResponse {
    private boolean success;
}