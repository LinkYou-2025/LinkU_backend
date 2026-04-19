package com.umc.linkyou.service.email;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.redis.PasswordResetCache;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.redis.PasswordResetRedisRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Pattern PASSWORD_POLICY_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,64}$");

    private final AuthAccountRepository authAccountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetRedisRepository passwordResetRedisRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;

    @Value("${app.server.base-url}")
    private String serverBaseUrl;

    private static final int EXPIRY_MINUTES = 10;
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration DAILY_LIMIT_TTL = Duration.ofDays(1);
    private static final int MAX_DAILY_SEND_COUNT = 5;
    private static final String SEND_COOLDOWN_KEY = "password:reset:cooldown:";
    private static final String DAILY_SEND_COUNT_KEY = "password:reset:count:";

    @Transactional
    public void sendResetLink(String email) {
        enforceResetRequestRateLimit(email);
        authAccountRepository.findUserByEmailAndProvider(email, Provider.GENERAL)
                .ifPresent(user -> sendResetEmail(email, user));
    }

    private void sendResetEmail(String email, Users user) {
        String token = UUID.randomUUID().toString();
        passwordResetRedisRepository.save(PasswordResetCache.of(token, email));

        String nickname = (user.getNickName() == null || user.getNickName().isBlank()) ? "링큐 회원" : user.getNickName();
        String resetUrl = serverBaseUrl + "/password/reset?token=" + token;
        try {
            emailService.sendPasswordResetEmail(email, nickname, resetUrl, EXPIRY_MINUTES);
        } catch (Exception e) {
            try {
                passwordResetRedisRepository.deleteById(token);
            } catch (Exception redisEx) {
                // 토큰 TTL로 자동 만료되므로 정리 실패는 로그만 남김
                log.error("발송 실패 후 토큰 정리 실패: {}",  redisEx);
            }
            throw e;
        }
        log.info("비밀번호 재설정 링크 전송: {}", user.getId());
    }

    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        validatePasswords(newPassword, confirmPassword);

        if (!newPassword.equals(confirmPassword)) {
            throw new UserHandler(ErrorStatus._PASSWORD_MISMATCH);
        }

        PasswordResetCache cache = passwordResetRedisRepository.findById(token)
                .orElseThrow(() -> new UserHandler(ErrorStatus._EXPIRED_VERIFICATION_CODE));

        Users user = authAccountRepository.findUserByEmailAndProvider(cache.getEmail(), Provider.GENERAL)
                .orElseThrow(() -> new UserHandler(ErrorStatus._USER_NOT_FOUND));

        user.encodePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetRedisRepository.deleteById(token);
        log.info("비밀번호 재설정 완료: {}", user.getId());
    }

    private void validatePasswords(String newPassword, String confirmPassword) {
        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            throw new UserHandler(ErrorStatus._INVALID_PASSWORD);
        }
        if (!PASSWORD_POLICY_PATTERN.matcher(newPassword).matches()) {
            throw new UserHandler(ErrorStatus._INVALID_PASSWORD);
        }
    }

    private void enforceResetRequestRateLimit(String email) {
        String cooldownKey = SEND_COOLDOWN_KEY + email;
        Boolean cooldownApplied = stringRedisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", SEND_COOLDOWN);
        if (!Boolean.TRUE.equals(cooldownApplied)) {
            log.warn("비밀번호 재설정 요청 차단 - cooldown active email={}", email);
            throw new UserHandler(ErrorStatus._TOO_MANY_REQUESTS);
        }

        String dailyCountKey = DAILY_SEND_COUNT_KEY + email;
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyCountKey);
        if (dailyCount == null) {
            log.error("비밀번호 재설정 일일 카운터 증가 실패 email={}", email);
            throw new UserHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
        if (dailyCount == 1L) {
            stringRedisTemplate.expire(dailyCountKey, DAILY_LIMIT_TTL);
        }
        if (dailyCount > MAX_DAILY_SEND_COUNT) {
            log.warn("비밀번호 재설정 요청 차단 - daily limit exceeded email={}, count={}", email, dailyCount);
            throw new UserHandler(ErrorStatus._TOO_MANY_REQUESTS);
        }

        log.info("비밀번호 재설정 요청 카운터 갱신 email={}, count={}", email, dailyCount);
    }
}
