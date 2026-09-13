package com.umc.linkyou.config.properties;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("email.resend")
public record ResendProperties(@NotBlank String apiKey, @NotBlank String from) {
    @Override
    public String toString() {
        return "ResendProperties[apiKey=***, from=" + from + "]";
    }
}
