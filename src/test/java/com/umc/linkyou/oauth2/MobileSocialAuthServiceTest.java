package com.umc.linkyou.oauth2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth2.mobile.client.GoogleTokenVerifier;
import com.umc.linkyou.oauth2.mobile.client.KakaoTokenClient;
import com.umc.linkyou.oauth2.mobile.client.NaverTokenClient;
import com.umc.linkyou.oauth2.mobile.dto.MobileLoginResponse;
import com.umc.linkyou.oauth2.mobile.service.GoogleMobileAuthService;
import com.umc.linkyou.oauth2.mobile.service.KakaoMobileAuthService;
import com.umc.linkyou.oauth2.mobile.service.NaverMobileAuthService;
import com.umc.linkyou.oauth2.utils.UserSocialLoginHelper;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.service.users.UserLoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MobileSocialAuthServiceTest {

    private static final String TOKEN = "provider-token";
    private static final String DEVICE_ID = "device-1";

    @Mock private KakaoTokenClient kakaoTokenClient;
    @Mock private GoogleTokenVerifier googleTokenVerifier;
    @Mock private NaverTokenClient naverTokenClient;
    @Mock private UserSocialLoginHelper userSocialLoginHelper;
    @Mock private UserLoginService userLoginService;
    @Mock private AuthAccountRepository authAccountRepository;

    private KakaoMobileAuthService kakaoService;
    private GoogleMobileAuthService googleService;
    private NaverMobileAuthService naverService;

    @BeforeEach
    void setUp() {
        kakaoService = new KakaoMobileAuthService(
                kakaoTokenClient, userSocialLoginHelper, userLoginService, authAccountRepository);
        googleService = new GoogleMobileAuthService(
                googleTokenVerifier, userSocialLoginHelper, userLoginService, authAccountRepository);
        naverService = new NaverMobileAuthService(
                naverTokenClient, userSocialLoginHelper, userLoginService, authAccountRepository);
    }

    @Nested
    @DisplayName("제공자별 로그인 성공")
    class SuccessfulLogin {

        @ParameterizedTest
        @CsvSource({
            "KAKAO,true",
            "KAKAO,false",
            "GOOGLE,true",
            "GOOGLE,false",
            "NAVER,true",
            "NAVER,false"
        })
        @DisplayName("제공자 이메일과 연결된 계정 이메일을 올바르게 전달한다")
        void 제공자_이메일과_연결된_계정_이메일을_올바르게_전달한다(
                Provider provider, boolean hasLinkedEmail) {
            SocialInfo info = stubProviderInfo(provider);
            Users user = Users.builder()
                    .id(42L)
                    .role(Role.USER)
                    .status(UserStatus.ACTIVE)
                    .build();
            when(userSocialLoginHelper.findOrCreateUser(
                            info.email(), info.name(), info.externalId(), info.profileImage(), provider))
                    .thenReturn(user);
            String storedEmail = hasLinkedEmail ? "stored@example.com" : null;
            if (hasLinkedEmail) {
                when(authAccountRepository.findByUserIdAndProvider(42L, provider))
                        .thenReturn(java.util.Optional.of(AuthAccount.builder()
                                .user(user)
                                .email(storedEmail)
                                .provider(provider)
                                .externalId(info.externalId())
                                .build()));
            } else {
                when(authAccountRepository.findByUserIdAndProvider(42L, provider))
                        .thenReturn(java.util.Optional.empty());
            }
            String resolvedEmail = hasLinkedEmail ? storedEmail : info.email();
            MobileLoginResponse expected = MobileLoginResponse.builder()
                    .userId(42L)
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .status(UserStatus.ACTIVE)
                    .build();
            when(userLoginService.handleSocialLogin(
                            user, resolvedEmail, provider, DEVICE_ID, DeviceType.PHONE))
                    .thenReturn(expected);

            MobileLoginResponse result = loginFor(provider);

            assertEquals(expected.getUserId(), result.getUserId());
            assertEquals(expected.getAccessToken(), result.getAccessToken());
            assertEquals(expected.getRefreshToken(), result.getRefreshToken());
            assertEquals(expected.getStatus(), result.getStatus());
            verify(userSocialLoginHelper).findOrCreateUser(
                    info.email(), info.name(), info.externalId(), info.profileImage(), provider);
            verify(authAccountRepository).findByUserIdAndProvider(42L, provider);
            verify(userLoginService).handleSocialLogin(
                    user, resolvedEmail, provider, DEVICE_ID, DeviceType.PHONE);
        }
    }

    @Nested
    @DisplayName("제공자 토큰 오류")
    class InvalidToken {

        @ParameterizedTest
        @EnumSource(value = Provider.class, names = {"KAKAO", "GOOGLE", "NAVER"})
        @DisplayName("토큰 검증 실패를 전달하고 계정 처리를 시작하지 않는다")
        void 토큰_검증_실패를_전달하고_계정_처리를_시작하지_않는다(Provider provider) {
            GeneralException tokenError = new GeneralException(ErrorStatus._INVALID_ID_TOKEN);
            stubProviderFailure(provider, tokenError);

            GeneralException ex = assertThrows(GeneralException.class, () -> loginFor(provider));

            assertEquals(ErrorStatus._INVALID_ID_TOKEN, ex.getCode());
            verifyNoInteractions(userSocialLoginHelper, userLoginService, authAccountRepository);
        }
    }

    private SocialInfo stubProviderInfo(Provider provider) {
        return switch (provider) {
            case KAKAO -> {
                KakaoTokenClient.KakaoUserInfo info = new KakaoTokenClient.KakaoUserInfo(
                        "kakao-id", "provider@example.com", "Kakao Name", "kakao-image");
                when(kakaoTokenClient.getUserInfo(TOKEN)).thenReturn(info);
                yield new SocialInfo(info.externalId(), info.email(), info.name(), info.profileImage());
            }
            case GOOGLE -> {
                GoogleTokenVerifier.GoogleUserInfo info = new GoogleTokenVerifier.GoogleUserInfo(
                        "google-id", "provider@example.com", "Google Name", "google-image");
                when(googleTokenVerifier.verify(TOKEN)).thenReturn(info);
                yield new SocialInfo(info.externalId(), info.email(), info.name(), info.profileImage());
            }
            case NAVER -> {
                NaverTokenClient.NaverUserInfo info = new NaverTokenClient.NaverUserInfo(
                        "naver-id", "provider@example.com", "Naver Name", "naver-image");
                when(naverTokenClient.getUserInfo(TOKEN)).thenReturn(info);
                yield new SocialInfo(info.externalId(), info.email(), info.name(), info.profileImage());
            }
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 제공자: " + provider);
        };
    }

    private void stubProviderFailure(Provider provider, GeneralException error) {
        switch (provider) {
            case KAKAO -> when(kakaoTokenClient.getUserInfo(TOKEN)).thenThrow(error);
            case GOOGLE -> when(googleTokenVerifier.verify(TOKEN)).thenThrow(error);
            case NAVER -> when(naverTokenClient.getUserInfo(TOKEN)).thenThrow(error);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 제공자: " + provider);
        }
    }

    private MobileLoginResponse loginFor(Provider provider) {
        return switch (provider) {
            case KAKAO -> kakaoService.login(TOKEN, DEVICE_ID, DeviceType.PHONE);
            case GOOGLE -> googleService.login(TOKEN, DEVICE_ID, DeviceType.PHONE);
            case NAVER -> naverService.login(TOKEN, DEVICE_ID, DeviceType.PHONE);
            default -> throw new IllegalArgumentException("지원하지 않는 소셜 제공자: " + provider);
        };
    }

    private record SocialInfo(String externalId, String email, String name, String profileImage) {}
}
