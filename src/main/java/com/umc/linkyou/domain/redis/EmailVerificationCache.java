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
@RedisHash(value = "email_verification", timeToLive = 600)
public class EmailVerificationCache {

    @Id
    private String emailHash;

    private String code;

    public static EmailVerificationCache of(String emailHash, String code) {
        return EmailVerificationCache.builder()
                .emailHash(emailHash)
                .code(code)
                .build();
    }
}
