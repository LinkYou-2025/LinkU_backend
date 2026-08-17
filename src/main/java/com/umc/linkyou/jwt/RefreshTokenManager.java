package com.umc.linkyou.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenManager {

    private final StringRedisTemplate redisTemplate; //Redis 서버와 실제로 통신하는 객체
    private final ObjectMapper objectMapper;  //JSON 직렬화/역직렬화
    private static final int MAX_SESSION = 3; //동시 접속 인원 = 3

    // 세션초과체크 + 만료세션정리 + 동일기기갱신 + 신규추가 + TTL설정
    private static final String ADD_TOKEN_SCRIPT = """
        local key = KEYS[1]
        local deviceId = ARGV[1]
        local newValue = ARGV[2]
        local maxCount = tonumber(ARGV[3])
        local ttlMs = tonumber(ARGV[4])
        local nowMs = tonumber(ARGV[5])
        local blacklistPrefix = ARGV[6]
        local blacklistReason = ARGV[7]

        local function blacklistInvalidatedSession(value)
            if value == nil then
                return
            end

            local decoded = cjson.decode(value)
            if decoded.sessionId ~= nil and decoded.accessExpiresAt ~= nil then
                local accessTtlMs = tonumber(decoded.accessExpiresAt) - nowMs
                if accessTtlMs > 0 then
                    redis.call('SET', blacklistPrefix .. decoded.sessionId, blacklistReason, 'PX', accessTtlMs)
                end
            end
        end

        local tokens = redis.call('HGETALL', key)
        local count = 0
        local oldestField = nil
        local oldestTime = math.huge

        for i = 1, #tokens, 2 do
            local field = tokens[i]
            local val = cjson.decode(tokens[i + 1])
            if val.expiresAt ~= nil and tonumber(val.expiresAt) <= nowMs then
                redis.call('HDEL', key, field)
            else
                count = count + 1
                if field ~= deviceId and tonumber(val.createdAt) < oldestTime then
                    oldestTime = tonumber(val.createdAt)
                    oldestField = field
                end
            end
        end

        local invalidatedValue = nil

        if redis.call('HEXISTS', key, deviceId) == 1 then
            invalidatedValue = redis.call('HGET', key, deviceId)
            blacklistInvalidatedSession(invalidatedValue)
            redis.call('HSET', key, deviceId, newValue)
            redis.call('PEXPIRE', key, ttlMs)
            return invalidatedValue
        end

        if count >= maxCount then
            if oldestField then
                invalidatedValue = redis.call('HGET', key, oldestField)
                blacklistInvalidatedSession(invalidatedValue)
                redis.call('HDEL', key, oldestField)
            end
        end

        redis.call('HSET', key, deviceId, newValue)
        redis.call('PEXPIRE', key, ttlMs)

        return invalidatedValue
    """;

    public Optional<InvalidatedSession> saveToken(
            Long userId,
            String provider,
            String deviceId,
            String tokenId,
            DeviceType deviceType,
            long expiresAt,
            String sessionId,
            long accessExpiresAt
    ) {
        String key = buildKey(userId, provider);
        long now = System.currentTimeMillis();
        long ttlMs = Math.max(1L, expiresAt - now);
        Map<String, Object> tokenData = Map.of(
                "tokenId", tokenId,
                "deviceType", deviceType.name(),
                "sessionId", sessionId,
                "accessExpiresAt", accessExpiresAt,
                "createdAt", now,
                "expiresAt", expiresAt
        );

        String tokenJson;
        try {
            tokenJson = objectMapper.writeValueAsString(tokenData);
        } catch (Exception e) {
            throw new UserHandler(AuthErrorStatus._REFRESH_TOKEN_SESSION_SAVE_ERROR);
        }

        String invalidatedRaw = redisTemplate.execute(
                new DefaultRedisScript<>(ADD_TOKEN_SCRIPT, String.class),
                List.of(key),
                deviceId,
                tokenJson,
                String.valueOf(MAX_SESSION),
                String.valueOf(ttlMs),
                String.valueOf(now),
                AccessTokenBlackListManager.SESSION_BLACKLIST_PREFIX,
                AccessTokenBlackListManager.BLACKLIST_REASON_LOGOUT
        );

        return toInvalidatedSession(invalidatedRaw);
    }

    // 토큰 검증
    public boolean validateToken(Long userId, String provider, String deviceId, String tokenId) {
        Map<String, Object> sessionData = getSessionData(userId, provider, deviceId);
        String storedTokenId = (String) sessionData.get("tokenId");
        Long expiresAt = toLong(sessionData.get("expiresAt"));

        if (storedTokenId == null || storedTokenId.isBlank() || expiresAt == null) {
            throw new UserHandler(UserErrorStatus._REFRESH_TOKEN_SESSION_INVALID);
        }

        return storedTokenId.equals(tokenId) && expiresAt > System.currentTimeMillis();
    }

    // provider와 관련된 전체 기기에서 로그아웃
    public void deleteAllTokens(Long userId) {
        List<String> keys = new ArrayList<>();
        for (Provider provider : Provider.values()) {
            keys.add(buildKey(userId, provider.name()));
        }
        redisTemplate.delete(keys);
    }

    // deviceId에 해당하는 AccessToken과 RefreshToken을 삭제
    public void deleteTokenForDevice(Long userId, String deviceId) {
        List<String> keys = new ArrayList<>();
        for (Provider provider : Provider.values()) {
            keys.add(buildKey(userId, provider.name()));
        }

        for (String key : keys) {
            redisTemplate.opsForHash().delete(key, deviceId);
        }
    }


    private static final String CONSUME_TOKEN_SCRIPT = """
        local key = KEYS[1]
        local deviceId = ARGV[1]
        local expectedTokenId = ARGV[2]
        local nowMs = tonumber(ARGV[3])

        local value = redis.call('HGET', key, deviceId)
        if value == false then
            return nil
        end

        local decoded = cjson.decode(value)
        if decoded.tokenId ~= expectedTokenId then
            return nil
        end

        if decoded.expiresAt ~= nil and tonumber(decoded.expiresAt) <= nowMs then
            redis.call('HDEL', key, deviceId)
            return nil
        end

        redis.call('HDEL', key, deviceId)
        return value
    """;

    public DeviceType consumeToken(Long userId, String provider, String deviceId, String tokenId) {
        String key = buildKey(userId, provider);

        String raw = redisTemplate.execute(
                new DefaultRedisScript<>(CONSUME_TOKEN_SCRIPT, String.class),
                List.of(key),
                deviceId,
                tokenId,
                String.valueOf(System.currentTimeMillis())
        );

        if (raw == null) {
            throw new UserHandler(UserErrorStatus._INVALID_REFRESH_TOKEN);
        }

        try {
            Map<String, Object> map = objectMapper.readValue(raw, Map.class);
            String storedTokenId = (String) map.get("tokenId");
            Long expiresAt = toLong(map.get("expiresAt"));
            String deviceType = (String) map.get("deviceType");
            if (storedTokenId == null || storedTokenId.isBlank() || expiresAt == null || deviceType == null || deviceType.isBlank()) {
                throw new UserHandler(UserErrorStatus._REFRESH_TOKEN_SESSION_INVALID);
            }
            return DeviceType.valueOf(deviceType);
        } catch (UserHandler e) {
            throw e;
        } catch (Exception e) {
            throw new UserHandler(UserErrorStatus._REFRESH_TOKEN_PARSE_ERROR);
        }
    }

    private Map<String, Object> getSessionData(Long userId, String provider, String deviceId) {
        String key = buildKey(userId, provider);
        Object raw = redisTemplate.opsForHash().get(key, deviceId);

        if (raw == null) {
            throw new UserHandler(UserErrorStatus._INVALID_REFRESH_TOKEN);
        }

        try {
            return objectMapper.readValue(raw.toString(), Map.class);
        } catch (Exception e) {
            throw new UserHandler(UserErrorStatus._REFRESH_TOKEN_PARSE_ERROR);
        }
    }

    private Optional<InvalidatedSession> toInvalidatedSession(String raw) {
        if (raw == null) {
            return Optional.empty();
        }

        Map<String, Object> sessionData;
        try {
            sessionData = objectMapper.readValue(raw, Map.class);
        } catch (Exception e) {
            throw new UserHandler(UserErrorStatus._REFRESH_TOKEN_PARSE_ERROR);
        }

        boolean hasSessionId = sessionData.containsKey("sessionId");
        boolean hasAccessExpiresAt = sessionData.containsKey("accessExpiresAt");

        if (!hasSessionId && !hasAccessExpiresAt) {
            if (isValidLegacySession(sessionData)) {
                return Optional.empty();
            }
            throw new UserHandler(UserErrorStatus._REFRESH_TOKEN_SESSION_INVALID);
        }

        String sessionId;
        Long accessExpiresAt;
        try {
            sessionId = (String) sessionData.get("sessionId");
            accessExpiresAt = toLong(sessionData.get("accessExpiresAt"));
        } catch (Exception e) {
            throw new UserHandler(UserErrorStatus._REFRESH_TOKEN_PARSE_ERROR);
        }

        if (sessionId == null || sessionId.isBlank() || accessExpiresAt == null) {
            throw new UserHandler(UserErrorStatus._REFRESH_TOKEN_SESSION_INVALID);
        }

        return Optional.of(new InvalidatedSession(sessionId, accessExpiresAt));
    }

    private boolean isValidLegacySession(Map<String, Object> sessionData) {
        try {
            return isLegacySession(sessionData);
        } catch (Exception e) {
            throw new UserHandler(UserErrorStatus._REFRESH_TOKEN_PARSE_ERROR);
        }
    }

    private boolean isLegacySession(Map<String, Object> sessionData) {
        String tokenId = (String) sessionData.get("tokenId");
        String deviceType = (String) sessionData.get("deviceType");
        Long createdAt = toLong(sessionData.get("createdAt"));
        Long expiresAt = toLong(sessionData.get("expiresAt"));

        return tokenId != null
                && !tokenId.isBlank()
                && deviceType != null
                && !deviceType.isBlank()
                && createdAt != null
                && expiresAt != null;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String buildKey(Long userId, String provider) {
        return "sessions:" + userId + ":" + provider;
    }

    public record InvalidatedSession(String sessionId, long accessExpiresAt) {
    }
}
