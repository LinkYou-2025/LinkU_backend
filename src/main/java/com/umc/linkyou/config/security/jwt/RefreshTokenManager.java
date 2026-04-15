package com.umc.linkyou.config.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;  //Java 객체 <-> JSON 문자열 변환
import com.umc.linkyou.domain.enums.Provider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;  //Key와 Value 모두 String 타입으로 고정된 Redis 템플릿
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenManager {

    private final StringRedisTemplate redisTemplate; //Redis 서버와 실제로 통신하는 객체
    private final ObjectMapper objectMapper;  //JSON 직렬화/역직렬화
    private static final int MAX_SESSION = 3; //동시 접속 인원 = 3

    //세션초과체크 + 삭제 + 신규추가 + TTL설정
    private static final String ADD_TOKEN_SCRIPT = """
        local key = KEYS[1]
        local newField = ARGV[1]
        local newValue = ARGV[2]
        local maxCount = tonumber(ARGV[3])
        local ttlMs = tonumber(ARGV[4])
        
        local tokens = redis.call('HGETALL', key)
        local count = #tokens / 2
        
        if count >= maxCount then
            local oldestField = nil
            local oldestTime = math.huge
            for i = 1, #tokens, 2 do
                local field = tokens[i]
                local val = cjson.decode(tokens[i+1])
                if val.createdAt < oldestTime then
                    oldestTime = val.createdAt
                    oldestField = field
                end
            end
            if oldestField then
                redis.call('HDEL', key, oldestField)
            end
        end
        
        redis.call('HSET', key, newField, newValue)
        redis.call('PEXPIRE', key, ttlMs)
        
        return 1
    """;

    public void saveToken(Long userId, String tokenId, String provider, long ttlMs) {
        String key = "sessions:" + userId;

        //Redis 키를 sessions:{userId} 형태로 구성
        Map<String, Object> tokenData = Map.of(
                "provider", provider,
                "createdAt", System.currentTimeMillis(),
                "ttl", ttlMs
        );

        String tokenJson;
        try {
            tokenJson = objectMapper.writeValueAsString(tokenData);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }

        redisTemplate.execute(
                new DefaultRedisScript<>(ADD_TOKEN_SCRIPT, Long.class),
                List.of(key),
                tokenId,
                tokenJson,
                String.valueOf(MAX_SESSION),
                String.valueOf(ttlMs)
        );
    }

    // validateToken: hasKey로 존재 여부 확인 (delete 아님)
    public boolean validateToken(Long userId, String tokenId) {
        String key = "sessions:" + userId;  // sessions로 통일
        return Boolean.TRUE.equals(
                redisTemplate.opsForHash().hasKey(key, tokenId)
        );
    }

    // deleteToken: 특정 토큰 하나만 삭제 (재발급 시 사용)
    public void deleteToken(Long userId, String tokenId) {
        String key = "sessions:" + userId;
        redisTemplate.opsForHash().delete(key, tokenId);
    }

    // getProvider: 재발급 시 provider 꺼내기
    public Optional<String> getProvider(Long userId, String tokenId) {
        String key = "sessions:" + userId;
        Object val = redisTemplate.opsForHash().get(key, tokenId);

        if (val == null) return Optional.empty();

        try {
            Map<String, Object> map = objectMapper.readValue(val.toString(), Map.class);
            String provider = (String) map.get("provider");

            if (provider == null || provider.isBlank()) return Optional.empty();

            return Optional.of(provider);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public void deleteAllTokens(Long userId) {
        String key = "sessions:" + userId;
        redisTemplate.delete(key);
    }

    // 토큰조회 + 삭제 :HGET으로 value 읽고 HDEL로 삭제를 원자적으로 처리
    private static final String CONSUME_TOKEN_SCRIPT = """
    local key = KEYS[1]
    local field = ARGV[1]
    
    local value = redis.call('HGET', key, field)
    if value == false then
        return nil
    end
    redis.call('HDEL', key, field)
    return value
""";

    // provider를 반환하면서 동시에 토큰을 삭제 (원자적)
    public Optional<String> consumeToken(Long userId, String tokenId) {
        String key = "sessions:" + userId;

        String raw = redisTemplate.execute(
                new DefaultRedisScript<>(CONSUME_TOKEN_SCRIPT, String.class),
                List.of(key),
                tokenId
        );

        if (raw == null) return Optional.empty();

        try {
            Map<String, Object> map = objectMapper.readValue(raw, Map.class);
            String provider = (String) map.get("provider");
            if (provider == null || provider.isBlank()) return Optional.empty();
            return Optional.of(provider);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
