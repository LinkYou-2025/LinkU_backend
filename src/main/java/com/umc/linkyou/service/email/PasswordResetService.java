package com.umc.linkyou.service.email;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 비밀번호 재설정 기능을 담당하는 서비스
 * 이메일로 재설정 링크를 발송하고, 토큰 검증 후 비밀번호를 업데이트하는 로직을 포함
 * 이메일 발송 시 쿨다운과 일일 전송 횟수 제한을 적용하여 남용을 방지
 * Redis에는 재설정 토큰과 이메일을 매핑하여 저장하며, 토큰은 일정 시간이 지나면 자동으로 만료
 */
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
    private final EmailService emailService;
    private final EmailRateLimiter rateLimiter;

    @Value("${app.server.base-url}")
    private String serverBaseUrl;

    private static final int EXPIRY_MINUTES = 10;
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final Duration DAILY_LIMIT_TTL = Duration.ofDays(1);
    private static final int MAX_DAILY_SEND_COUNT = 5;
    private static final String SEND_COOLDOWN_KEY = "password:reset:cooldown:";
    private static final String DAILY_SEND_COUNT_KEY = "password:reset:count:";

    // 비밀번호 재설정 링크 전송
    @Transactional(readOnly = true)
    public void sendResetLink(String email) {
        rateLimiter.enforce(email, SEND_COOLDOWN_KEY, DAILY_SEND_COUNT_KEY,
                SEND_COOLDOWN, DAILY_LIMIT_TTL, MAX_DAILY_SEND_COUNT);
        authAccountRepository.findUserByEmailAndProvider(email, Provider.GENERAL)
                .ifPresent(user -> sendResetEmail(email, user));
    }

    // 비밀번호 재설정 이메일 발송 및 Redis에 토큰 저장
    private void sendResetEmail(String email, Users user) {
        String token = UUID.randomUUID().toString();
        passwordResetRedisRepository.save(PasswordResetCache.of(token, email));

        String nickname = (user.getNickName() == null || user.getNickName().isBlank()) ? "링큐 회원" : user.getNickName();
        String resetUrl = serverBaseUrl + "/password/reset?token=" + token;
        try {
            // 이메일 발송 실패 시 Redis에 저장된 토큰 삭제해서 재설정 시도 불가하도록 함
            emailService.sendPasswordResetEmail(email, nickname, resetUrl, EXPIRY_MINUTES);
        } catch (Exception e) {
            try {
                // 이메일 발송 실패 시 토큰 정리 시도, 실패해도 TTL로 자동 만료되므로 재시도 방지 가능
                passwordResetRedisRepository.deleteById(token);
            } catch (Exception redisEx) {
                // 토큰 TTL로 자동 만료되므로 정리 실패는 로그만 남김
                log.error("발송 실패 후 토큰 정리 실패", redisEx);
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

        // 토큰으로 이메일 조회, 토큰 없거나 만료되면 에러
        PasswordResetCache cache = passwordResetRedisRepository.findById(token)
                .orElseThrow(() -> new UserHandler(UserErrorStatus._EXPIRED_VERIFICATION_CODE));

        // 이메일로 사용자 조회, 일반 로그인 계정이 아니거나 가입되지 않은 이메일이면 에러
        Users user = authAccountRepository.findUserByEmailAndProvider(cache.getEmail(), Provider.GENERAL)
                .orElseThrow(() -> new UserHandler(UserErrorStatus._USER_NOT_FOUND));

        // 새 비밀번호로 업데이트해서 저장
        user.encodePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 재설정 완료했으므로 토큰 삭제
        passwordResetRedisRepository.deleteById(token);
        log.info("비밀번호 재설정 완료: {}", user.getId());
    }

    // 새 비밀번호 유효성 검사
    private void validatePasswords(String newPassword, String confirmPassword) {
        if (!StringUtils.hasText(newPassword) || !StringUtils.hasText(confirmPassword)) {
            throw new UserHandler(ErrorStatus._INVALID_PASSWORD);
        }
        if (!PASSWORD_POLICY_PATTERN.matcher(newPassword).matches()) {
            throw new UserHandler(ErrorStatus._INVALID_PASSWORD);
        }
    }
}
