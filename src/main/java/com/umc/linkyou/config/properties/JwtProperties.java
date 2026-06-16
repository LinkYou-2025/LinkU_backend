package com.umc.linkyou.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("jwt.token")
public record JwtProperties(
        Keys keys,
        String issuer,
        Expiration expiration
) {
    public record Expiration(Long access, Long refresh) {}
    public record Keys(String access, String refresh) {}
}
