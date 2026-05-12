package com.umc.linkyou.jwt;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

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
    private static final String BLACKLIST_REASON_LOGOUT = "logout";
    private final StringRedisTemplate redisTemplate;

    // 로그아웃 , 탈퇴시 블랙리스트로 등록(엑세스 토큰 남은 만료 시간만큼 ttl 설정)
    public void addToBlacklist(String token, long ttlMs) {
        String key = blacklistKey(token);
        redisTemplate.opsForValue().set(key, BLACKLIST_REASON_LOGOUT, Duration.ofMillis(ttlMs));
    }

    // 토큰이 블랙리스트에 있는지 확인
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        return redisTemplate.hasKey(blacklistKey(token));
    }

    private String blacklistKey(String token) {
        String normalizedToken = jwtTokenProvider.normalizeStrict(token);
        return BLACKLIST_PREFIX + sha256Hex(normalizedToken);
    }

    private String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }

}
