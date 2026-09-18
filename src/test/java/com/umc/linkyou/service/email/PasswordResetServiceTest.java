package com.umc.linkyou.service.email;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.domain.redis.PasswordResetCache;
import jakarta.mail.internet.AddressException;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.redis.PasswordResetRedisRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.AuthProvider;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static com.umc.linkyou.service.email.EmailVerificationService.*;
import static com.umc.linkyou.service.email.PasswordResetService.MAX_DAILY_SEND_COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.*;

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

    private Users userWithStatus(UserStatus status) {
        return Users.builder()
                .id(1L)
                .nickName("링큐유저")
                .password("encoded")
                .status(status)
                .build();
    }

    private AuthAccount accountOf(Provider provider, Users user) {
        return AuthAccount.builder()
                .email("user@example.com")
                .provider(provider)
                .user(user)
                .build();
    }

    @Nested
    @DisplayName("정상 케이스")
    class Success {

        @Test
        @DisplayName("일반 계정이면 비밀번호 재설정 링크를 전송한다")
        void 일반_계정이면_재설정_링크를_전송한다() throws AddressException {
            given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);

            Users user = userWithStatus(UserStatus.ACTIVE);
            AuthAccount authAccount = AuthAccount.builder()
                    .email("user@example.com")
                    .provider(Provider.GENERAL)
                    .user(user)
                    .build();
            given(authAccountRepository.findByEmail("user@example.com"))
                    .willReturn(List.of(authAccount));

            given(authAccountRepository.findUserByEmailAndProvider("user@example.com", Provider.GENERAL))
                    .willReturn(Optional.of(user));

            passwordResetService.sendResetLink("user@example.com");

            verify(passwordResetRedisRepository).save(any());
            verify(emailService).sendPasswordResetEmail(eq("user@example.com"), eq("링큐유저"), any(), eq(10));
        }

        @Test
        @DisplayName("일반 계정과 카카오 계정이 함께 있으면 비밀번호 재설정 링크를 전송한다")
        void 일반_계정과_카카오_계정이_함께_있으면_재설정_링크를_전송한다() throws AddressException {
            given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);

            Users user = userWithStatus(UserStatus.ACTIVE);
            given(authAccountRepository.findByEmail("user@example.com"))
                    .willReturn(List.of(accountOf(Provider.KAKAO, user), accountOf(Provider.GENERAL, user)));
            given(authAccountRepository.findUserByEmailAndProvider("user@example.com", Provider.GENERAL))
                    .willReturn(Optional.of(user));

            passwordResetService.sendResetLink("user@example.com");

            verify(passwordResetRedisRepository).save(any());
            verify(emailService).sendPasswordResetEmail(eq("user@example.com"), eq("링큐유저"), any(), eq(10));
        }

        @Test
        @DisplayName("일반 계정과 구글 계정이 함께 있으면 비밀번호 재설정 링크를 전송한다")
        void 일반_계정과_구글_계정이_함께_있으면_재설정_링크를_전송한다() throws AddressException {
            given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);

            Users user = userWithStatus(UserStatus.ACTIVE);
            given(authAccountRepository.findByEmail("user@example.com"))
                    .willReturn(List.of(accountOf(Provider.GENERAL, user), accountOf(Provider.GOOGLE, user)));
            given(authAccountRepository.findUserByEmailAndProvider("user@example.com", Provider.GENERAL))
                    .willReturn(Optional.of(user));

            passwordResetService.sendResetLink("user@example.com");

            verify(passwordResetRedisRepository).save(any());
            verify(emailService).sendPasswordResetEmail(eq("user@example.com"), eq("링큐유저"), any(), eq(10));
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class Failure {

        @Test
        @DisplayName("유효하지 않은 이메일 주소면 비밀번호 재설정 링크를 전송하지 않는다")
        void 유효하지_않은_이메일_주소면_재설정_링크를_전송하지_않는다() throws AddressException {
            given(emailAddressValidator.isDeliverableAddress("user@invalid-domain.invalid")).willReturn(false);

            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.sendResetLink("user@invalid-domain.invalid"));

            assertEquals(UserErrorStatus._INVALID_EMAIL_ADDRESS, exception.getCode());
            verifyNoInteractions(authAccountRepository, passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
        }


        @Test
        @DisplayName("카카오 소셜 로그인으로 가입한 계정이면 에러를 던진다")
        void 카카오_소셜_로그인으로_가입한_계정이면_에러를_던진다() throws AddressException {

            given(emailAddressValidator.isDeliverableAddress("user@example.com"))
                    .willReturn(true);

            AuthAccount authAccount = AuthAccount.builder()
                    .email("user@example.com")
                    .provider(Provider.KAKAO)
                    .user(userWithStatus(UserStatus.ACTIVE))
                    .build();

            given(authAccountRepository.findByEmail("user@example.com"))
                    .willReturn(List.of(authAccount));

            UserHandler exception = assertThrows(
                    UserHandler.class,
                    () -> passwordResetService.sendResetLink("user@example.com")
            );

            assertEquals(
                    UserErrorStatus._KAKAO_SOCIAL_ACCOUNT_ALREADY_EXISTS,
                    exception.getCode()
            );

            verify(authAccountRepository)
                    .findByEmail("user@example.com");

            verify(authAccountRepository, never())
                    .findUserByEmailAndProvider("user@example.com", Provider.GENERAL);

            verifyNoInteractions(
                    emailService,
                    userRepository,
                    passwordEncoder
            );
        }

        @Test
        @DisplayName("비밀번호 재설정 요청이 cooldown 중이면 차단한다")
        void 재설정_요청이_cooldown_중이면_차단한다() throws AddressException {

            given(emailAddressValidator.isDeliverableAddress("user@example.com"))
                    .willReturn(true);

            doThrow(new UserHandler(ErrorStatus._TOO_MANY_REQUESTS))
                    .when(rateLimiter)
                    .enforce(
                            eq("user@example.com"),
                            eq("password:reset:cooldown:"),
                            eq("password:reset:count:"),
                            eq(Duration.ofMinutes(1)),
                            eq(Duration.ofHours(24)),
                            eq(5)
                    );

            UserHandler exception = assertThrows(
                    UserHandler.class,
                    () -> passwordResetService.sendResetLink("user@example.com")
            );

            assertEquals(
                    ErrorStatus._TOO_MANY_REQUESTS,
                    exception.getCode()
            );

            verify(rateLimiter).enforce(
                    eq("user@example.com"),
                    eq("password:reset:cooldown:"),
                    eq("password:reset:count:"),
                    eq(Duration.ofMinutes(1)),
                    eq(Duration.ofHours(24)),
                    eq(5)
            );

            verifyNoInteractions(
                    authAccountRepository,
                    passwordResetRedisRepository,
                    emailService,
                    userRepository,
                    passwordEncoder
            );
        }

        @Test
        @DisplayName("비밀번호 재설정 요청이 일일 제한을 넘기면 차단한다")
        void 재설정_요청이_일일_제한을_넘기면_차단한다() throws AddressException {
            given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);

            willThrow(new UserHandler(ErrorStatus._TOO_MANY_REQUESTS))
                    .given(rateLimiter).enforce(anyString(), anyString(), anyString(), any(), any(), anyInt());

            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.sendResetLink("user@example.com"));

            assertEquals(ErrorStatus._TOO_MANY_REQUESTS, exception.getCode());
            verifyNoInteractions(authAccountRepository, passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("가입되지 않은 이메일이면 사용자 없음 예외를 던진다")
        void 가입되지_않은_이메일이면_사용자_없음_예외를_던진다() throws AddressException {
            given(emailAddressValidator.isDeliverableAddress("missing@example.com")).willReturn(true);
            given(authAccountRepository.findByEmail("missing@example.com"))
                    .willReturn(List.of());

            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.sendResetLink("missing@example.com"));

            assertEquals(UserErrorStatus._USER_NOT_FOUND, exception.getCode());
            verify(authAccountRepository).findByEmail("missing@example.com");
            verify(authAccountRepository, never())
                    .findUserByEmailAndProvider("missing@example.com", Provider.GENERAL);
            verifyNoInteractions(passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("구글 소셜 로그인으로만 가입한 계정이면 구글 에러를 던진다")
        void 구글_소셜_로그인으로만_가입한_계정이면_구글_에러를_던진다() throws AddressException {
            given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);
            Users user = userWithStatus(UserStatus.ACTIVE);
            given(authAccountRepository.findByEmail("user@example.com"))
                    .willReturn(List.of(accountOf(Provider.GOOGLE, user)));

            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.sendResetLink("user@example.com"));

            assertEquals(UserErrorStatus._GOOGLE_SOCIAL_ACCOUNT_ALREADY_EXISTS, exception.getCode());
            verify(authAccountRepository, never())
                    .findUserByEmailAndProvider("user@example.com", Provider.GENERAL);
            verifyNoInteractions(passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("카카오와 구글 소셜 로그인으로 모두 가입한 계정이면 통합 소셜 에러를 던진다")
        void 카카오와_구글_소셜_로그인으로_모두_가입한_계정이면_통합_소셜_에러를_던진다() throws AddressException {
            given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);
            Users user = userWithStatus(UserStatus.ACTIVE);
            given(authAccountRepository.findByEmail("user@example.com"))
                    .willReturn(List.of(accountOf(Provider.KAKAO, user), accountOf(Provider.GOOGLE, user)));

            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.sendResetLink("user@example.com"));

            assertEquals(UserErrorStatus._KAKAO_GOOGLE_SOCIAL_ACCOUNT_ALREADY_EXISTS, exception.getCode());
            verify(authAccountRepository, never())
                    .findUserByEmailAndProvider("user@example.com", Provider.GENERAL);
            verifyNoInteractions(passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("임시(TEMP) 상태 계정이면 사용자 없음 예외를 던진다")
        void 임시_상태_계정이면_사용자_없음_예외를_던진다() throws AddressException {
            given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);
            AuthAccount authAccount = AuthAccount.builder()
                    .email("user@example.com")
                    .provider(Provider.GENERAL)
                    .user(userWithStatus(UserStatus.TEMP))
                    .build();
            given(authAccountRepository.findByEmail("user@example.com"))
                    .willReturn(List.of(authAccount));

            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.sendResetLink("user@example.com"));

            assertEquals(UserErrorStatus._USER_NOT_FOUND, exception.getCode());
            verify(authAccountRepository, never())
                    .findUserByEmailAndProvider("user@example.com", Provider.GENERAL);
            verifyNoInteractions(passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("탈퇴(INACTIVE) 상태 계정이면 사용자 없음 예외를 던진다")
        void 탈퇴_상태_계정이면_사용자_없음_예외를_던진다() throws AddressException {
            given(emailAddressValidator.isDeliverableAddress("user@example.com")).willReturn(true);
            AuthAccount authAccount = AuthAccount.builder()
                    .email("user@example.com")
                    .provider(Provider.GENERAL)
                    .user(userWithStatus(UserStatus.INACTIVE))
                    .build();
            given(authAccountRepository.findByEmail("user@example.com"))
                    .willReturn(List.of(authAccount));

            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.sendResetLink("user@example.com"));

            assertEquals(UserErrorStatus._USER_NOT_FOUND, exception.getCode());
            verify(authAccountRepository, never())
                    .findUserByEmailAndProvider("user@example.com", Provider.GENERAL);
            verifyNoInteractions(passwordResetRedisRepository, emailService, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("재설정 시점에 ACTIVE 상태가 아니면 사용자 없음 예외를 던진다")
        void 재설정_시점에_ACTIVE_상태가_아니면_사용자_없음_예외를_던진다() {
            given(passwordResetRedisRepository.findById("token"))
                    .willReturn(Optional.of(PasswordResetCache.of("token", "user@example.com")));
            given(authAccountRepository.findUserByEmailAndProvider("user@example.com", Provider.GENERAL))
                    .willReturn(Optional.of(userWithStatus(UserStatus.INACTIVE)));

            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.resetPassword("token", "Valid123!", "Valid123!"));

            assertEquals(UserErrorStatus._USER_NOT_FOUND, exception.getCode());
            verify(passwordResetRedisRepository, never()).deleteById(anyString());
            verifyNoInteractions(userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("새 비밀번호가 비어 있으면 잘못된 비밀번호 예외를 던진다")
        void 새_비밀번호가_비어있으면_예외를_던진다() {
            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.resetPassword("token", " ", "Valid123!"));

            assertEquals(UserErrorStatus._INVALID_PASSWORD, exception.getCode());
            verifyNoInteractions(passwordResetRedisRepository, authAccountRepository, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("새 비밀번호가 정책에 맞지 않으면 잘못된 비밀번호 예외를 던진다")
        void 새_비밀번호가_정책에_맞지_않으면_예외를_던진다() {
            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.resetPassword("token", "short1!", "short1!"));

            assertEquals(UserErrorStatus._INVALID_PASSWORD, exception.getCode());
            verifyNoInteractions(passwordResetRedisRepository, authAccountRepository, userRepository, passwordEncoder);
        }

        @Test
        @DisplayName("새 비밀번호에 대문자가 없으면 잘못된 비밀번호 예외를 던진다")
        void 새_비밀번호에_대문자가_없으면_예외를_던진다() {
            UserHandler exception = assertThrows(UserHandler.class,
                    () -> passwordResetService.resetPassword("token", "password123!", "password123!"));

            assertEquals(UserErrorStatus._INVALID_PASSWORD, exception.getCode());
            verifyNoInteractions(passwordResetRedisRepository, authAccountRepository, userRepository, passwordEncoder);
        }
    }
}
