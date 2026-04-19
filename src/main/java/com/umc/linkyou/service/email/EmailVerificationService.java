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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Locale;
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

        String hashedEmail = hashEmail(email);
        String code = generateCode();
        emailVerificationRedisRepository.save(EmailVerificationCache.of(hashedEmail, code));

        emailService.sendVerificationEmailTemplate(email, "링큐 회원", code, EXPIRY_MINUTES);
        try {
            emailService.sendPasswordResetEmail(email, "링큐 회원", code, EXPIRY_MINUTES);
            resetVerifyFailureCount(hashedEmail);
        } catch (Exception e) {
            log.error("이메일 인증 코드 전송 실패 ", e);
            emailVerificationRedisRepository.deleteById(hashedEmail);
            throw new UserHandler(ErrorStatus._SEND_MAIL_FAILED);
        }
        log.info("이메일 인증 코드 전송 완료");
    }

    // 이메일 인증 코드 검증
    // Redis에 코드가 없으면 만료, 코드 불일치면 검증 실패
    public void verifyCode(String email, String code) {
        String hashedEmail = hashEmail(email);
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
            throw new UserHandler(ErrorStatus._VERIFICATION_FAILED);
        }

        emailVerificationRedisRepository.deleteById(hashedEmail);
        resetVerifyFailureCount(hashedEmail);
        log.info("이메일 인증 코드 검증 성공");
    }

    private void enforceSendRateLimit(String email) {
        String hashedEmail = hashEmail(email);
        String cooldownKey = SEND_COOLDOWN_KEY + hashedEmail;
        Boolean cooldownApplied = stringRedisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", SEND_COOLDOWN);
        if (!Boolean.TRUE.equals(cooldownApplied)) {
            log.warn("이메일 인증 코드 전송 차단 - cooldown active");
            throw new UserHandler(ErrorStatus._TOO_MANY_REQUESTS);
        }

        String dailyCountKey = DAILY_SEND_COUNT_KEY + hashedEmail;
        Long dailyCount = stringRedisTemplate.opsForValue().increment(dailyCountKey);
        if (dailyCount == null) {
            log.error("이메일 인증 일일 카운터 증가 실패");
            throw new UserHandler(ErrorStatus._INTERNAL_SERVER_ERROR);
        }
        if (dailyCount == 1L) {
            stringRedisTemplate.expire(dailyCountKey, DAILY_LIMIT_TTL);
        }
        if (dailyCount > MAX_DAILY_SEND_COUNT) {
            log.warn("이메일 인증 코드 전송 차단 - daily limit exceeded count={}", dailyCount);
            throw new UserHandler(ErrorStatus._TOO_MANY_REQUESTS);
        }

        log.info("이메일 인증 전송 카운터 갱신 count={}", dailyCount);
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

    private static String hashEmail(String email) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(email.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
