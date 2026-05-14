package com.umc.linkyou.converter;

import com.umc.linkyou.domain.EmailVerification;

import java.time.LocalDateTime;

public class EmailConverter {
    public static EmailVerification toEmailVerification(String email, String code, Boolean bool) {
        return EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .isVerified(false)
                .build();
    }
}
