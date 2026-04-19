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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final EmailService emailService;

    @Value("${app.server.base-url}")
    private String serverBaseUrl;

    private static final int EXPIRY_MINUTES = 10;

    @Transactional
    public void sendResetLink(String email) {
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
}
