package com.umc.linkyou.domain.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("fcm")
public class UserFcmTokenCache {

    @Id
    private Long userId;

    @Builder.Default
    private Set<String> activeTokens = new HashSet<>();

    @TimeToLive(unit = TimeUnit.DAYS)
    @Builder.Default
    private long ttl = 60L;

    public void addToken(String token) {
        this.activeTokens.add(token);
    }

    public void removeToken(String token) {
        this.activeTokens.remove(token);
    }
}
