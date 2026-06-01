package com.umc.linkyou.service.email;

import jakarta.mail.internet.AddressException;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.domain.redis.EmailVerificationCache;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.redis.EmailVerificationRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    // SHA-256("test@example.com")
    private static final String HASHED_EMAIL = "973dfe463ec85785f5f95af5ba3906eedb2d931c24e69824a89ea65dba4e813b";

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Mock
    private AuthAccountRepository authAccountRepository;

    @Mock
    private EmailVerificationRedisRepository emailVerificationRedisRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailRateLimiter rateLimiter;

    @Mock
    private EmailDomainValidator emailAddressValidator;

    @Test
    @DisplayName("유효하지 않은 이메일 주소면 인증 코드를 전송하지 않는다")
    void sendCode_whenEmailAddressInvalid_throwsBadRequest() throws AddressException {
        given(emailAddressValidator.isDeliverableAddress("test@invalid-domain.invalid")).willReturn(false);

        UserHandler exception = assertThrows(UserHandler.class,
                () -> emailVerificationService.sendCode("test@invalid-domain.invalid"));

        assertEquals(UserErrorStatus._INVALID_EMAIL_ADDRESS, exception.getCode());
        verifyNoInteractions(authAccountRepository, emailVerificationRedisRepository, emailService);
    }

    @Test
    @DisplayName("cooldown 중이면 인증 코드를 전송하지 않는다")
    void sendCode_whenCooldownActive_throwsTooManyRequests() throws AddressException {
        given(emailAddressValidator.isDeliverableAddress("test@example.com")).willReturn(true);
        given(authAccountRepository.existsByEmail("test@example.com")).willReturn(false);
        willThrow(new UserHandler(ErrorStatus._TOO_MANY_REQUESTS))
                .given(rateLimiter).enforce(anyString(), anyString(), anyString(), any(), any(), anyInt());

        UserHandler exception = assertThrows(UserHandler.class,
                () -> emailVerificationService.sendCode("test@example.com"));

        assertEquals(ErrorStatus._TOO_MANY_REQUESTS, exception.getCode());
        verifyNoInteractions(emailVerificationRedisRepository, emailService);
    }

    @Test
    @DisplayName("일일 제한을 넘기면 인증 코드를 전송하지 않는다")
    void sendCode_whenDailyLimitExceeded_throwsTooManyRequests() throws AddressException {
        given(emailAddressValidator.isDeliverableAddress("test@example.com")).willReturn(true);
        given(authAccountRepository.existsByEmail("test@example.com")).willReturn(false);
        willThrow(new UserHandler(ErrorStatus._TOO_MANY_REQUESTS))
                .given(rateLimiter).enforce(anyString(), anyString(), anyString(), any(), any(), anyInt());

        UserHandler exception = assertThrows(UserHandler.class,
                () -> emailVerificationService.sendCode("test@example.com"));

        assertEquals(ErrorStatus._TOO_MANY_REQUESTS, exception.getCode());
        verifyNoInteractions(emailVerificationRedisRepository, emailService);
    }

    @Test
    @DisplayName("제한 이내면 인증 코드를 저장하고 메일을 전송한다")
    void sendCode_whenAllowed_savesCodeAndSendsEmail() throws AddressException {
        given(emailAddressValidator.isDeliverableAddress("test@example.com")).willReturn(true);
        given(authAccountRepository.existsByEmail("test@example.com")).willReturn(false);

        emailVerificationService.sendCode("test@example.com");

        verify(emailVerificationRedisRepository).save(any());
        verify(emailService).sendVerificationEmailTemplate(eq("test@example.com"), eq("링큐 회원"), anyString(), eq(10));
    }

    @Test
    @DisplayName("인증 코드가 틀리면 실패 카운터를 증가시키고 검증 실패 예외를 던진다")
    void verifyCode_whenCodeMismatch_incrementsFailureCount() {
        given(emailVerificationRedisRepository.findById(HASHED_EMAIL))
                .willReturn(java.util.Optional.of(EmailVerificationCache.of(HASHED_EMAIL, "123456")));
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("email:verification:failure:" + HASHED_EMAIL)).willReturn(1L);

        UserHandler exception = assertThrows(UserHandler.class,
                () -> emailVerificationService.verifyCode("test@example.com", "000000"));

        assertEquals(UserErrorStatus._VERIFICATION_FAILED, exception.getCode());
        verify(stringRedisTemplate).expire(eq("email:verification:failure:" + HASHED_EMAIL), any());
    }

    @Test
    @DisplayName("인증 코드 실패가 5회째면 코드를 만료 처리한다")
    void verifyCode_whenFailureCountReached_expiresCode() {
        given(emailVerificationRedisRepository.findById(HASHED_EMAIL))
                .willReturn(java.util.Optional.of(EmailVerificationCache.of(HASHED_EMAIL, "123456")));
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.increment("email:verification:failure:" + HASHED_EMAIL)).willReturn(5L);

        UserHandler exception = assertThrows(UserHandler.class,
                () -> emailVerificationService.verifyCode("test@example.com", "000000"));

        assertEquals(UserErrorStatus._EXPIRED_VERIFICATION_CODE, exception.getCode());
        verify(emailVerificationRedisRepository).deleteById(HASHED_EMAIL);
        verify(stringRedisTemplate).delete("email:verification:failure:" + HASHED_EMAIL);
    }

    @Test
    @DisplayName("인증 코드가 맞으면 실패 카운터를 초기화한다")
    void verifyCode_whenSuccess_resetsFailureCount() {
        given(emailVerificationRedisRepository.findById(HASHED_EMAIL))
                .willReturn(java.util.Optional.of(EmailVerificationCache.of(HASHED_EMAIL, "123456")));

        emailVerificationService.verifyCode("test@example.com", "123456");

        verify(emailVerificationRedisRepository).deleteById(HASHED_EMAIL);
        verify(stringRedisTemplate).delete("email:verification:failure:" + HASHED_EMAIL);
    }
}
