package com.umc.linkyou.service.email;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.domain.redis.EmailVerificationCache;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.redis.EmailVerificationRedisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final AuthAccountRepository authAccountRepository;
    private final EmailVerificationRedisRepository emailVerificationRedisRepository;
    private final EmailService emailService;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int EXPIRY_MINUTES = 10;
    private static final int CODE_LENGTH = 6;

    // 회원가입 이메일 인증 코드 전송
    // 이미 가입된 이메일이면 중복 에러
    public void sendCode(String email) {
        if (authAccountRepository.existsByEmail(email)) {
            throw new UserHandler(ErrorStatus._DUPLICATE_JOIN_REQUEST);
        }

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

        if (!cache.getCode().equals(code)) {
            throw new UserHandler(ErrorStatus._VERIFICATION_FAILED);
        }
        emailVerificationRedisRepository.deleteById(email);
    }

    private String generateCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            builder.append(SECURE_RANDOM.nextInt(10));
        }
        return builder.toString();
    }
}
