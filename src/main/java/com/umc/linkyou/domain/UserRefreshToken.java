package com.umc.linkyou.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;
import org.springframework.data.redis.core.index.Indexed;

import java.util.concurrent.TimeUnit;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@RedisHash(value = "refreshToken")
public class UserRefreshToken {
    @Id
    private String refreshToken;
    @Indexed
    private Long userId;

    @TimeToLive(unit = TimeUnit.MILLISECONDS)
    private Long ttl;

    public UserRefreshToken(String refreshToken, Long userId, Long ttl) {
        this.refreshToken = refreshToken;
        this.userId = userId;
        this.ttl = ttl;
    }
}