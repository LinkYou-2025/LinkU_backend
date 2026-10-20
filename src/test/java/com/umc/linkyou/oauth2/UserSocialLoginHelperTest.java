package com.umc.linkyou.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth2.utils.UserSocialLoginHelper;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserSocialLoginHelperTest {

    @InjectMocks private UserSocialLoginHelper userSocialLoginHelper;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("소셜 계정을 찾거나 생성할 때")
    class FindOrCreateUser {

        @Nested
        @DisplayName("신규 가입")
        class NewAccount {

            @ParameterizedTest
            @EnumSource(value = Provider.class, names = {"KAKAO", "GOOGLE", "NAVER"})
            @DisplayName("제공자 이름 대신 LINKU 임시 닉네임을 저장한다")
            void 제공자_이름_대신_LINKU_임시_닉네임을_저장한다(Provider provider) {
                String email = "new-user@example.com";
                String externalId = provider.name().toLowerCase() + "_new_user";
                when(authAccountRepository.findByProviderAndExternalId(provider, externalId))
                        .thenReturn(Optional.empty());
                when(authAccountRepository.findUserByEmailExcludingProvider(email, provider))
                        .thenReturn(Optional.empty());
                when(userRepository.saveAndFlush(any(Users.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

                Users result = userSocialLoginHelper.findOrCreateUser(
                        email, "소셜 프로필 이름", externalId, "photo.jpg", provider);

                assertTrue(result.getNickName().matches("LINKU_[0-9a-f]{32}"));
                assertNotEquals("소셜 프로필 이름", result.getNickName());
                assertEquals(UserStatus.TEMP, result.getStatus());
                assertTrue(result.getNickName().length() <= 255);
                verify(userRepository).saveAndFlush(any(Users.class));
            }
        }

        @Nested
        @DisplayName("기존 계정")
        class ExistingAccount {

            @Test
            @DisplayName("기존 소셜 계정 재로그인 시 사용자를 유지하고 소셜 토큰을 갱신한다")
            void 기존_소셜_계정_재로그인_시_사용자를_유지하고_토큰을_갱신한다() {
                Users existingUser = Users.builder()
                        .id(10L)
                        .nickName("기존닉네임")
                        .status(UserStatus.ACTIVE)
                        .build();
                AuthAccount existingAccount = AuthAccount.builder()
                        .user(existingUser)
                        .provider(Provider.KAKAO)
                        .externalId("kakao_existing")
                        .email("old@example.com")
                        .socialToken("old-token")
                        .build();
                when(authAccountRepository.findByProviderAndExternalId(
                                Provider.KAKAO, "kakao_existing"))
                        .thenReturn(Optional.of(existingAccount));

                Users result = userSocialLoginHelper.findOrCreateUser(
                        "new@example.com", "변경된 이름", "kakao_existing", "new-photo.jpg",
                        Provider.KAKAO, "new-token");

                assertEquals(existingUser, result);
                assertEquals("기존닉네임", result.getNickName());
                assertEquals("new-token", existingAccount.getSocialToken());
                verify(authAccountRepository, never()).save(any(AuthAccount.class));
                verify(userRepository, never()).saveAndFlush(any(Users.class));
            }

            @Test
            @DisplayName("모바일 재로그인 시 저장된 소셜 토큰을 null로 덮어쓰지 않는다")
            void 모바일_재로그인_시_소셜_토큰을_유지한다() {
                AuthAccount existingAccount = AuthAccount.builder()
                        .user(Users.builder().id(11L).nickName("기존닉네임").build())
                        .provider(Provider.KAKAO)
                        .externalId("kakao_mobile")
                        .email("user@example.com")
                        .socialToken("previous-token")
                        .build();
                when(authAccountRepository.findByProviderAndExternalId(
                                Provider.KAKAO, "kakao_mobile"))
                        .thenReturn(Optional.of(existingAccount));

                userSocialLoginHelper.findOrCreateUser(
                        "user@example.com", "이름 변경", "kakao_mobile", "new-image",
                        Provider.KAKAO);

                assertEquals("previous-token", existingAccount.getSocialToken());
                verify(authAccountRepository, never()).save(any(AuthAccount.class));
            }

            @Test
            @DisplayName("다른 제공자로 가입된 동일 이메일 계정에 소셜 로그인을 연결한다")
            void 다른_제공자로_가입된_동일_이메일_계정에_로그인을_연결한다() {
                Users existingUser = Users.builder().id(20L).nickName("기존닉네임").build();
                when(authAccountRepository.findByProviderAndExternalId(
                                Provider.GOOGLE, "google_1"))
                        .thenReturn(Optional.empty());
                when(authAccountRepository.findUserByEmailExcludingProvider(
                                "same@example.com", Provider.GOOGLE))
                        .thenReturn(Optional.of(existingUser));

                Users result = userSocialLoginHelper.findOrCreateUser(
                        "same@example.com", "Google Name", "google_1", "image.jpg",
                        Provider.GOOGLE, "google-token");

                assertEquals(existingUser, result);
                verify(authAccountRepository).save(org.mockito.ArgumentMatchers.argThat(account ->
                        account.getUser().equals(existingUser)
                                && account.getProvider() == Provider.GOOGLE
                                && account.getExternalId().equals("google_1")
                                && account.getEmail().equals("same@example.com")
                                && account.getSocialToken().equals("google-token")
                                && account.getProfileImage().equals("image.jpg")));
                verify(userRepository, never()).saveAndFlush(any(Users.class));
            }
        }

        @Nested
        @DisplayName("필수 식별자 오류")
        class InvalidIdentifiers {

            @Test
            @DisplayName("이메일이 없으면 필수 이메일 오류를 반환한다")
            void 이메일이_없으면_필수_이메일_오류를_반환한다() {
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> userSocialLoginHelper.findOrCreateUser(
                                " ", "이름", "external-id", null, Provider.KAKAO));
                assertEquals(ErrorStatus._SOCIAL_EMAIL_REQUIRED, ex.getCode());
                verifyNoInteractions(authAccountRepository, userRepository);
            }

            @Test
            @DisplayName("이메일 형식이 잘못되면 형식 오류를 반환한다")
            void 이메일_형식이_잘못되면_형식_오류를_반환한다() {
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> userSocialLoginHelper.findOrCreateUser(
                                "invalid-email", "이름", "external-id", null, Provider.KAKAO));
                assertEquals(ErrorStatus._INVALID_EMAIL_FORMAT, ex.getCode());
                verifyNoInteractions(authAccountRepository, userRepository);
            }

            @Test
            @DisplayName("외부 사용자 식별자가 없으면 필수 식별자 오류를 반환한다")
            void 외부_사용자_식별자가_없으면_필수_식별자_오류를_반환한다() {
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> userSocialLoginHelper.findOrCreateUser(
                                "user@example.com", "이름", " ", null, Provider.KAKAO));
                assertEquals(ErrorStatus._SOCIAL_EXTERNAL_ID_REQUIRED, ex.getCode());
                verifyNoInteractions(authAccountRepository, userRepository);
            }

            @Test
            @DisplayName("제공자가 없으면 지원하지 않는 제공자 오류를 반환한다")
            void 제공자가_없으면_지원하지_않는_제공자_오류를_반환한다() {
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> userSocialLoginHelper.findOrCreateUser(
                                "user@example.com", "이름", "external-id", null, null));
                assertEquals(ErrorStatus._SOCIAL_UNSUPPORTED_PROVIDER, ex.getCode());
                verifyNoInteractions(authAccountRepository, userRepository);
            }
        }
    }

    @Test
    @DisplayName("기존 일반 계정 이메일로 소셜 로그인하면 기존 사용자에 계정을 연결한다")
    void 기존_일반_계정_이메일로_소셜_로그인하면_기존_사용자에_계정을_연결한다() {
        String email = "test@example.com";
        String externalId = "kakao_12345";
        Users existingUser = Users.builder().id(10L).nickName("일반유저").build();
        when(authAccountRepository.findByProviderAndExternalId(Provider.KAKAO, externalId))
                .thenReturn(Optional.empty());
        when(authAccountRepository.findUserByEmailExcludingProvider(email, Provider.KAKAO))
                .thenReturn(Optional.of(existingUser));

        Users result = userSocialLoginHelper.findOrCreateUser(
                email, "카카오닉네임", externalId, "photo.jpg", Provider.KAKAO);

        assertEquals(10L, result.getId());
        verify(authAccountRepository).save(any(AuthAccount.class));
    }

    @Test
    @DisplayName("소셜 계정 연결 저장 실패 시 전용 오류 코드를 반환한다")
    void 소셜_계정_연결_저장_실패_시_전용_오류_코드를_반환한다() {
        String email = "new-user@example.com";
        when(authAccountRepository.findByProviderAndExternalId(Provider.KAKAO, "kakao_2"))
                .thenReturn(Optional.empty());
        when(authAccountRepository.findUserByEmailExcludingProvider(email, Provider.KAKAO))
                .thenReturn(Optional.of(Users.builder().id(21L).nickName("사용자").build()));
        doThrow(new IllegalStateException("database unavailable"))
                .when(authAccountRepository)
                .save(any(AuthAccount.class));

        GeneralException ex = assertThrows(GeneralException.class,
                () -> userSocialLoginHelper.findOrCreateUser(
                        email, "카카오 이름", "kakao_2", null, Provider.KAKAO));

        assertEquals(ErrorStatus._AUTH_ACCOUNT_SAVE_FAILED, ex.getCode());
    }
}
