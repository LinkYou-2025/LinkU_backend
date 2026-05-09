package com.umc.linkyou.jwt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AccessToken 블랙리스트 관리 클래스
 * - 로그아웃 또는 회원 탈퇴 시 해당 AccessToken을 블랙리스트에 등록하여 더 이상 사용할 수 없도록 함
 * - Redis를 사용하여 블랙리스트를 관리하며, 토큰의 남은 만료 시간만큼 TTL을 설정하여 자동으로 블랙리스트에서 제거되도록 함
 */
@Component
@RequiredArgsConstructor
public class AccessTokenBlackListManager {

    private final JwtTokenProvider jwtTokenProvider;
    private static final String BLACKLIST_PREFIX = "blacklist:access:";
    private final StringRedisTemplate redisTemplate;

    // 로그아웃 , 탈퇴시 블랙리스트로 등록(엑세스 토큰 남은 만료 시간만큼 ttl 설정)
    public void addToBlacklist(String token, String deviceId, long ttlMs) {
        String key = BLACKLIST_PREFIX + jwtTokenProvider.normalizeStrict(token);
        redisTemplate.opsForValue().set(key, deviceId, Duration.ofMillis(ttlMs));
    }

    // 토큰이 블랙리스트에 있는지 확인
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        return redisTemplate.hasKey(BLACKLIST_PREFIX + jwtTokenProvider.normalizeStrict(token));
    }

}
