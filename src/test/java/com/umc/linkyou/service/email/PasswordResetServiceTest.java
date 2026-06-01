package com.umc.linkyou.service.email;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.redis.PasswordResetRedisRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @InjectMocks
    private PasswordResetService passwordResetService;

    @Mock
    private AuthAccountRepository authAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordResetRedisRepository passwordResetRedisRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private EmailRateLimiter rateLimiter;

    @Mock
    private EmailDomainValidator emailAddressValidator;

    @Test
    @DisplayName("유효하지 않은 이메일 주소면 비밀번호 재설정 링크를 전송하지 않는다")
    void sendResetLink_whenEmailAddressInvalid_throwsBadRequest() {
        given(emailAddressValidator.isDeliverableAddress("user@invalid-domain.invalid")).willReturn(false);

        UserHandler exception = assertThrows(UserHandler.class,
                () -> passwordResetService.sendResetLink("user@invalid-domain.invalid"));

        assertEquals(UserErrorStatus._INVALID_EMAIL_ADDRESS, exception.getCode());
        verifyNoInteractions(authAccountRepository, passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("비밀번호 재설정 요청이 cooldown 중이면 차단한다")
    void sendResetLink_whenCooldownActive_throwsTooManyRequests() {
        given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);
        willThrow(new UserHandler(ErrorStatus._TOO_MANY_REQUESTS))
                .given(rateLimiter).enforce(anyString(), anyString(), anyString(), any(), any(), anyInt());

        UserHandler exception = assertThrows(UserHandler.class,
                () -> passwordResetService.sendResetLink("user@example.com"));

        assertEquals(ErrorStatus._TOO_MANY_REQUESTS, exception.getCode());
        verifyNoInteractions(authAccountRepository, passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("비밀번호 재설정 요청이 일일 제한을 넘기면 차단한다")
    void sendResetLink_whenDailyLimitExceeded_throwsTooManyRequests() {
        given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);
        willThrow(new UserHandler(ErrorStatus._TOO_MANY_REQUESTS))
                .given(rateLimiter).enforce(anyString(), anyString(), anyString(), any(), any(), anyInt());

        UserHandler exception = assertThrows(UserHandler.class,
                () -> passwordResetService.sendResetLink("user@example.com"));

        assertEquals(ErrorStatus._TOO_MANY_REQUESTS, exception.getCode());
        verifyNoInteractions(authAccountRepository, passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("일반 계정이 없으면 비밀번호 재설정 링크를 보내지 않고 성공처럼 종료한다")
    void sendResetLink_absentGeneralAccount_returnsSilently() {
        given(emailAddressValidator.isDeliverableAddress("missing@example.com")).willReturn(true);
        given(authAccountRepository.findUserByEmailAndProvider("missing@example.com", com.umc.linkyou.domain.enums.Provider.GENERAL))
                .willReturn(Optional.empty());

        passwordResetService.sendResetLink("missing@example.com");

        verify(authAccountRepository).findUserByEmailAndProvider("missing@example.com", com.umc.linkyou.domain.enums.Provider.GENERAL);
        verifyNoInteractions(passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("일반 계정이면 비밀번호 재설정 링크를 전송한다")
    void sendResetLink_generalAccount_sendsResetEmail() {
        given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);
        Users user = Users.builder()
                .id(1L)
                .nickName("링큐유저")
                .password("encoded")
                .build();
        given(authAccountRepository.findUserByEmailAndProvider("user@example.com", com.umc.linkyou.domain.enums.Provider.GENERAL))
                .willReturn(Optional.of(user));

        passwordResetService.sendResetLink("user@example.com");

        verify(passwordResetRedisRepository).save(any());
        verify(emailService).sendPasswordResetEmail(eq("user@example.com"), eq("링큐유저"), any(), eq(10));
    }

    @Test
    @DisplayName("새 비밀번호가 비어 있으면 잘못된 비밀번호 예외를 던진다")
    void resetPassword_blankPassword_throwsInvalidPassword() {
        UserHandler exception = assertThrows(UserHandler.class,
                () -> passwordResetService.resetPassword("token", " ", "Valid123!"));

        assertEquals(UserErrorStatus._INVALID_PASSWORD, exception.getCode());
        verifyNoInteractions(passwordResetRedisRepository, authAccountRepository, userRepository, passwordEncoder);
    }

    @Test
    @DisplayName("새 비밀번호가 정책에 맞지 않으면 잘못된 비밀번호 예외를 던진다")
    void resetPassword_invalidPolicy_throwsInvalidPassword() {
        UserHandler exception = assertThrows(UserHandler.class,
                () -> passwordResetService.resetPassword("token", "short1!", "short1!"));

        assertEquals(UserErrorStatus._INVALID_PASSWORD, exception.getCode());
        verifyNoInteractions(passwordResetRedisRepository, authAccountRepository, userRepository, passwordEncoder);
    }
}
