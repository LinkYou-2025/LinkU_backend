package com.umc.linkyou.domain.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "password_reset", timeToLive = 600)
public class PasswordResetCache {

    @Id
    private String token;

    private String email;

    public static PasswordResetCache of(String token, String email) {
        return PasswordResetCache.builder()
                .token(token)
                .email(email)
                .build();
    }
}
