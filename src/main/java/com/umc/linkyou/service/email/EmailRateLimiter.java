package com.umc.linkyou.service.email;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;

/**
 * 이메일 전송에 대한 쿨다운과 일일 전송 횟수 제한을 관리하는 컴포넌트 - 재사용성을 위해 별도 클래스로 분리했습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    public void enforce(String email,
                        String cooldownKeyPrefix,
                        String dailyKeyPrefix,
                        Duration cooldown,
                        Duration dailyTtl,
                        int maxDailyCount) {
        String hashedEmail = hashEmail(email);

        Boolean cooldownApplied = stringRedisTemplate.opsForValue()
                .setIfAbsent(cooldownKeyPrefix + hashedEmail, "1", cooldown);
        if (!Boolean.TRUE.equals(cooldownApplied)) {
            log.warn("[{}] 전송 차단 - cooldown active", cooldownKeyPrefix);
            throw new UserHandler(ErrorStatus._TOO_MANY_REQUESTS);
        }

        String dailyCountKey = dailyKeyPrefix + hashedEmail;
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyCountKey);
        if (dailyCount == null) {
            log.error("[{}] 일일 카운터 증가 실패", dailyKeyPrefix);
            throw new UserHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
        if (dailyCount == 1L) {
            stringRedisTemplate.expire(dailyCountKey, dailyTtl);
        }
        if (dailyCount > maxDailyCount) {
            log.warn("[{}] 전송 차단 - daily limit exceeded count={}", dailyKeyPrefix, dailyCount);
            throw new UserHandler(ErrorStatus._TOO_MANY_REQUESTS);
        }

        log.info("[{}] 카운터 갱신 count={}", dailyKeyPrefix, dailyCount);
    }

    public static String hashEmail(String email) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(email.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
