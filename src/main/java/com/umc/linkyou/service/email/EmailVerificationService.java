package com.umc.linkyou.service.email;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.domain.redis.EmailVerificationCache;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.redis.EmailVerificationRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final AuthAccountRepository authAccountRepository;
    private final EmailVerificationRedisRepository emailVerificationRedisRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int EXPIRY_MINUTES = 10;
    private static final int CODE_LENGTH = 6;
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration DAILY_LIMIT_TTL = Duration.ofDays(1);
    private static final int MAX_DAILY_SEND_COUNT = 5;
    private static final int MAX_VERIFY_FAILURE_COUNT = 5;
    private static final String SEND_COOLDOWN_KEY = "email:verification:cooldown:";
    private static final String DAILY_SEND_COUNT_KEY = "email:verification:count:";
    private static final String VERIFY_FAILURE_COUNT_KEY = "email:verification:failure:";

    // 회원가입 이메일 인증 코드 전송
    // 이미 가입된 이메일이면 중복 에러
    public void sendCode(String email) {
        if (authAccountRepository.existsByEmail(email)) {
            throw new UserHandler(ErrorStatus._DUPLICATE_JOIN_REQUEST);
        }

        enforceSendRateLimit(email);

        String code = generateCode();
        emailVerificationRedisRepository.save(EmailVerificationCache.of(email, code));

        emailService.sendVerificationEmailTemplate(email, "링큐 회원", code, EXPIRY_MINUTES);
        log.info("이메일 인증 코드 전송 완료");
    }

    // 이메일 인증 코드 검증
    // Redis에 코드가 없으면 만료, 코드 불일치면 검증 실패
    public void verifyCode(String email, String code) {
        EmailVerificationCache cache = emailVerificationRedisRepository.findById(email)
                .orElseThrow(() -> new UserHandler(ErrorStatus._EXPIRED_VERIFICATION_CODE));

        if (!Objects.equals(cache.getCode(), code)) {
            long failureCount = increaseVerifyFailureCount(email);
            if (failureCount >= MAX_VERIFY_FAILURE_COUNT) {
                emailVerificationRedisRepository.deleteById(email);
                resetVerifyFailureCount(email);
                log.warn("이메일 인증 코드 검증 차단 - 최대 실패 횟수 초과 email={}, failureCount={}", email, failureCount);
                throw new UserHandler(ErrorStatus._EXPIRED_VERIFICATION_CODE);
            }
            log.warn("이메일 인증 코드 검증 실패 email={}, failureCount={}", email, failureCount);
            throw new UserHandler(ErrorStatus._VERIFICATION_FAILED);
        }

        emailVerificationRedisRepository.deleteById(email);
        resetVerifyFailureCount(email);
        log.info("이메일 인증 코드 검증 성공 email={}", email);
    }

    private void enforceSendRateLimit(String email) {
        String cooldownKey = SEND_COOLDOWN_KEY + email;
        Boolean cooldownApplied = stringRedisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", SEND_COOLDOWN);
        if (!Boolean.TRUE.equals(cooldownApplied)) {
            log.warn("이메일 인증 코드 전송 차단 - cooldown active email={}", email);
            throw new UserHandler(ErrorStatus._TOO_MANY_REQUESTS);
        }

        String dailyCountKey = DAILY_SEND_COUNT_KEY + email;
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyCountKey);
        if (dailyCount == null) {
            log.error("이메일 인증 일일 카운터 증가 실패 email={}", email);
            throw new UserHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
        if (dailyCount == 1L) {
            stringRedisTemplate.expire(dailyCountKey, DAILY_LIMIT_TTL);
        }
        if (dailyCount > MAX_DAILY_SEND_COUNT) {
            log.warn("이메일 인증 코드 전송 차단 - daily limit exceeded email={}, count={}", email, dailyCount);
            throw new UserHandler(ErrorStatus._TOO_MANY_REQUESTS);
        }

        log.info("이메일 인증 전송 카운터 갱신 email={}, count={}", email, dailyCount);
    }

    private long increaseVerifyFailureCount(String email) {
        String failureCountKey = VERIFY_FAILURE_COUNT_KEY + email;
        Long failureCount = stringRedisTemplate.opsForValue().increment(failureCountKey);
        if (failureCount == null) {
            log.error("이메일 인증 실패 카운터 증가 실패 email={}", email);
            throw new UserHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
        if (failureCount == 1L) {
            stringRedisTemplate.expire(failureCountKey, Duration.ofMinutes(EXPIRY_MINUTES));
        }
        return failureCount;
    }

    private void resetVerifyFailureCount(String email) {
        stringRedisTemplate.delete(VERIFY_FAILURE_COUNT_KEY + email);
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(SECURE_RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
