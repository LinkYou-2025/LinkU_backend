package com.umc.linkyou.service.email;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
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

/*
    * 이메일 인증 기능을 담당하는 서비스
    * 회원가입 시 이메일로 인증 코드를 발송하고, 사용자가 입력한 코드와 Redis에 저장된 코드를 비교하여 검증하는 로직을 포함
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final AuthAccountRepository authAccountRepository;
    private final EmailVerificationRedisRepository emailVerificationRedisRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final EmailService emailService;
    private final EmailRateLimiter rateLimiter;

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
            throw new UserHandler(UserErrorStatus._DUPLICATE_JOIN_REQUEST);
        }

        rateLimiter.enforce(email, SEND_COOLDOWN_KEY, DAILY_SEND_COUNT_KEY,
                SEND_COOLDOWN, DAILY_LIMIT_TTL, MAX_DAILY_SEND_COUNT);

        String hashedEmail = EmailRateLimiter.hashEmail(email);
        String code = generateCode();
        emailVerificationRedisRepository.save(EmailVerificationCache.of(hashedEmail, code));

        try {
            emailService.sendVerificationEmailTemplate(email, "링큐 회원", code, EXPIRY_MINUTES);
            resetVerifyFailureCount(hashedEmail);
        } catch (Exception e) {
            log.error("이메일 인증 코드 전송 실패", e);
            emailVerificationRedisRepository.deleteById(hashedEmail);
            throw new UserHandler(UserErrorStatus._SEND_MAIL_FAILED);
        }
        log.info("이메일 인증 코드 전송 완료");
    }

    // 이메일 인증 코드 검증
    // Redis에 코드가 없으면 만료, 코드 불일치면 검증 실패
    public void verifyCode(String email, String code) {
        String hashedEmail = EmailRateLimiter.hashEmail(email);
        EmailVerificationCache cache = emailVerificationRedisRepository.findById(hashedEmail)
                .orElseThrow(() -> new UserHandler(ErrorStatus._EXPIRED_VERIFICATION_CODE));

        if (!Objects.equals(cache.getCode(), code)) {
            long failureCount = increaseVerifyFailureCount(hashedEmail);
            if (failureCount >= MAX_VERIFY_FAILURE_COUNT) {
                emailVerificationRedisRepository.deleteById(hashedEmail);
                resetVerifyFailureCount(hashedEmail);
                log.warn("이메일 인증 코드 검증 차단 - 최대 실패 횟수 초과 failureCount={}", failureCount);
                throw new UserHandler(ErrorStatus._EXPIRED_VERIFICATION_CODE);
            }
            log.warn("이메일 인증 코드 검증 실패 failureCount={}", failureCount);
            throw new UserHandler(UserErrorStatus._VERIFICATION_FAILED);
        }

        emailVerificationRedisRepository.deleteById(hashedEmail);
        resetVerifyFailureCount(hashedEmail);
        log.info("이메일 인증 코드 검증 성공");
    }

    // 인증 코드 검증 실패 카운터 증가
    private long increaseVerifyFailureCount(String hashedEmail) {
        String failureCountKey = VERIFY_FAILURE_COUNT_KEY + hashedEmail;
        Long failureCount = stringRedisTemplate.opsForValue().increment(failureCountKey);
        if (failureCount == null) {
            log.error("이메일 인증 실패 카운터 증가 실패");
            throw new UserHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
        if (failureCount == 1L) {
            stringRedisTemplate.expire(failureCountKey, Duration.ofMinutes(EXPIRY_MINUTES));
        }
        return failureCount;
    }

    // 인증 코드 검증 실패 카운터 초기화
    private void resetVerifyFailureCount(String hashedEmail) {
        stringRedisTemplate.delete(VERIFY_FAILURE_COUNT_KEY + hashedEmail);
    }

    // 코드 생성
    private String generateCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(SECURE_RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
