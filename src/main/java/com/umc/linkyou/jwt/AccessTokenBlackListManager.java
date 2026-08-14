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
    private static final String BLACKLIST_PREFIX = "blacklist:access:session:";
    private static final String LEGACY_BLACKLIST_PREFIX = "blacklist:access:";
    private static final String BLACKLIST_REASON_LOGOUT = "logout";
    private final StringRedisTemplate redisTemplate;

    // 로그아웃 , 탈퇴시 블랙리스트로 등록(엑세스 토큰 남은 만료 시간만큼 ttl 설정)
    public void addToBlacklist(String token, long ttlMs) {
        String sessionId = jwtTokenProvider.getSessionId(token);
        String key = sessionId == null || sessionId.isBlank() ? legacyBlacklistKey(token) : blacklistKey(sessionId);
        redisTemplate.opsForValue().set(key, BLACKLIST_REASON_LOGOUT, Duration.ofMillis(ttlMs));
    }

    public void addSessionToBlacklist(String sessionId, long ttlMs) {
        if (sessionId == null || sessionId.isBlank() || ttlMs <= 0) {
            return;
        }

        redisTemplate.opsForValue().set(
                blacklistKey(sessionId), BLACKLIST_REASON_LOGOUT, Duration.ofMillis(ttlMs));
    }

    // 토큰이 블랙리스트에 있는지 확인
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String sessionId = jwtTokenProvider.getSessionId(token);
        String key = sessionId == null || sessionId.isBlank() ? legacyBlacklistKey(token) : blacklistKey(sessionId);
        return redisTemplate.hasKey(key);
    }

    private String blacklistKey(String sessionId) {
        return BLACKLIST_PREFIX + sessionId;
    }

    private String legacyBlacklistKey(String token) {
        String normalizedToken = jwtTokenProvider.normalizeStrict(token);
        return LEGACY_BLACKLIST_PREFIX + sha256Hex(normalizedToken);
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
