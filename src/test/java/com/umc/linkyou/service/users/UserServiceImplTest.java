package com.umc.linkyou.service.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.umc.linkyou.config.properties.JwtProperties;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.RefreshTokenManager;
import com.umc.linkyou.jwt.TokenIssueService;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.categoryRepository.UsersCategoryColorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.InterestRepository;
import com.umc.linkyou.repository.classification.JobRepository;
import com.umc.linkyou.repository.classification.PurposeRepository;
import com.umc.linkyou.repository.classification.UsersInterestRepository;
import com.umc.linkyou.repository.classification.UsersPurposeRepository;
import com.umc.linkyou.repository.userRepository.UserQueryRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserQueryRepository userQueryRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JobRepository jobRepository;
    @Mock private InterestRepository interestRepository;
    @Mock private PurposeRepository purposeRepository;
    @Mock private UsersInterestRepository usersInterestRepository;
    @Mock private UsersPurposeRepository usersPurposeRepository;
    @Mock private FolderRepository folderRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private UsersFolderRepository usersFolderRepository;
    @Mock private UsersCategoryColorRepository usersCategoryColorRepository;
    @Mock private RefreshTokenManager refreshTokenManager;
    @Mock private TokenIssueService tokenIssueService;
    @Mock private AccessTokenBlackListManager accessTokenBlackListManager;
    @Mock private UserStatusValidator userStatusValidator;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private JwtProperties jwtProperties;
    @Mock private AlarmSettingRepository alarmSettingRepository;
    @Mock private TermsAgreementService termsAgreementService;

    @Nested
    @DisplayName("일반 회원가입 (joinUser)")
    class JoinUser {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("성공 - 신규 유저 가입 시 기본 ROLE_USER 권한과 알림 설정이 생성된다")
            void join_user_success() {
                // given
                UserRequestDTO.JoinDTO request =
                        UserRequestDTO.JoinDTO.builder()
                                .nickName("테스터")
                                .email("test@example.com")
                                .password("password123")
                                .jobId(1L)
                                .gender(Gender.MALE)
                                .purposeList(new ArrayList<>())
                                .interestList(new ArrayList<>())
                                .termsMap(Collections.emptyMap())
                                .deviceId("test-device")
                                .deviceType(DeviceType.PHONE)
                                .build();

                Job mockJob = Job.builder().id(1L).build();

                when(userRepository.findByNickName(eq(request.nickName())))
                        .thenReturn(Optional.empty());
                when(jobRepository.findById(anyLong())).thenReturn(Optional.of(mockJob));
                when(authAccountRepository.existsByProviderAndExternalId(
                                eq(Provider.GENERAL), anyString()))
                        .thenReturn(false);
                when(authAccountRepository.findUserByEmailAndProvider(
                                anyString(), eq(Provider.GENERAL)))
                        .thenReturn(Optional.empty());
                when(userRepository.save(any(Users.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(categoryRepository.findAll()).thenReturn(new ArrayList<>());
                when(usersCategoryColorRepository.saveAll(anyList()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                when(tokenIssueService.issueTokenPair(any(), any(), any(), any(), any(), any()))
                        .thenReturn(
                                new TokenIssueService.IssuedTokenPair(
                                        "accessToken", "refreshToken"));

                // when
                UserResponseDTO.JoinResultDTO result = userService.joinUser(request);

                // then
                ArgumentCaptor<Users> savedUserCaptor = ArgumentCaptor.forClass(Users.class);
                verify(userRepository).save(savedUserCaptor.capture());
                assertNotNull(savedUserCaptor.getValue().getRole());
                assertEquals(Role.USER, savedUserCaptor.getValue().getRole());
                assertNotNull(result.getTokenResponse());
                verify(termsAgreementService).upsertTerms(any(Users.class), any());
                verify(alarmSettingRepository).save(any());
            }
        }
    }

    @Nested
    @DisplayName("소셜 프로필 완성 (socialCompleteProfile)")
    class SocialCompleteProfile {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("성공 - TEMP 상태 유저가 프로필 완성 시 ACTIVE 상태로 변경되고 정식 토큰이 발급된다")
            void social_complete_success() {
                // given
                Users tempUser = Users.builder().id(1L).status(UserStatus.TEMP).build();

                UserRequestDTO.SocialCompleteDTO request =
                        new UserRequestDTO.SocialCompleteDTO(
                                "완성닉네임",
                                Gender.MALE,
                                1L,
                                new ArrayList<>(),
                                new ArrayList<>(),
                                Collections.emptyMap(),
                                "test-device",
                                DeviceType.PHONE);

                when(userRepository.findById(eq(tempUser.getId())))
                        .thenReturn(Optional.of(tempUser));
                when(userRepository.findByNickName(eq("완성닉네임"))).thenReturn(Optional.empty());
                when(jobRepository.findById(anyLong()))
                        .thenReturn(Optional.of(Job.builder().id(1L).build()));
                when(userRepository.save(any(Users.class)))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                when(categoryRepository.findAll()).thenReturn(new ArrayList<>());
                when(usersCategoryColorRepository.saveAll(anyList()))
                        .thenAnswer(invocation -> invocation.getArgument(0));
                when(authAccountRepository.findEmailByUserIdAndProvider(eq(1L), eq(Provider.KAKAO)))
                        .thenReturn(Optional.of("kakao@example.com"));
                when(tokenIssueService.issueForStatus(
                                eq(1L),
                                eq("kakao@example.com"),
                                eq("KAKAO"),
                                any(),
                                eq(UserStatus.ACTIVE),
                                eq("test-device"),
                                eq(DeviceType.PHONE)))
                        .thenReturn(
                                new TokenIssueService.IssuedTokenPair(
                                        "accessToken", "refreshToken"));

                // when
                UserResponseDTO.JoinResultDTO result =
                        userService.socialCompleteProfile(tempUser.getId(), "KAKAO", request);

                // then
                assertEquals(UserStatus.ACTIVE, tempUser.getStatus());
                assertEquals("완성닉네임", tempUser.getNickName());
                assertEquals("accessToken", result.getTokenResponse().getAccessToken());
                assertEquals("refreshToken", result.getTokenResponse().getRefreshToken());
                verify(termsAgreementService).upsertTerms(any(Users.class), any());
            }
        }
    }

    @Nested
    @DisplayName("일반 로그인 (loginUser)")
    class LoginUser {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("성공 - 로그인 성공 시 토큰 페어와 유저 권한 정보가 반환된다")
            void login_user_success() {
                // given
                UserRequestDTO.LoginRequestDTO request =
                        new UserRequestDTO.LoginRequestDTO(
                                "test@example.com",
                                "password123",
                                "ios-iphone-16-pro",
                                DeviceType.PHONE);

                Users user =
                        Users.builder()
                                .id(1L)
                                .role(Role.USER)
                                .status(UserStatus.ACTIVE)
                                .password("encodedPassword")
                                .build();

                AuthAccount authAccount =
                        AuthAccount.builder().user(user).email("test@example.com").build();

                when(authAccountRepository.findUserByEmailAndProvider(
                                eq(request.email()), eq(Provider.GENERAL)))
                        .thenReturn(Optional.of(user));

                when(authAccountRepository.existsByUserIdAndProvider(
                                anyLong(), eq(Provider.GENERAL)))
                        .thenReturn(true);

                when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

                when(authAccountRepository.findByUserIdAndProvider(anyLong(), eq(Provider.GENERAL)))
                        .thenReturn(Optional.of(authAccount));

                TokenIssueService.IssuedTokenPair issuedTokenPair =
                        new TokenIssueService.IssuedTokenPair("mockAccess", "mockRefresh");

                when(tokenIssueService.issueTokenPair(
                                eq(user.getId()),
                                eq("test@example.com"),
                                eq(Provider.GENERAL.name()),
                                eq(Role.USER),
                                eq(request.deviceId()),
                                eq(request.deviceType())))
                        .thenReturn(issuedTokenPair);

                // when
                UserResponseDTO.LoginResultDTO result = userService.loginUser(request);

                // then
                assertNotNull(result.getAccessToken());
                verify(userStatusValidator).validateLoginAllowed(user);
                verify(tokenIssueService)
                        .issueTokenPair(
                                eq(user.getId()),
                                eq("test@example.com"),
                                eq(Provider.GENERAL.name()),
                                eq(Role.USER),
                                eq(request.deviceId()),
                                eq(request.deviceType()));
            }

            @Test
            @DisplayName("성공 - 탈퇴 유예 기간 이내인 유저는 복구 전용 토큰만 발급받는다")
            void 탈퇴_유예_기간_이내인_유저는_복구_전용_토큰만_발급받는다() {
                // given
                UserRequestDTO.LoginRequestDTO request =
                        new UserRequestDTO.LoginRequestDTO(
                                "inactive@example.com",
                                "password123",
                                "ios-iphone-16-pro",
                                DeviceType.PHONE);

                Users user =
                        Users.builder()
                                .id(2L)
                                .role(Role.USER)
                                .status(UserStatus.INACTIVE)
                                .password("encodedPassword")
                                .build();
                user.withdraw("테스트 탈퇴", java.time.LocalDateTime.now().minusDays(3));

                AuthAccount authAccount =
                        AuthAccount.builder().user(user).email("inactive@example.com").build();

                when(authAccountRepository.findUserByEmailAndProvider(
                                eq(request.email()), eq(Provider.GENERAL)))
                        .thenReturn(Optional.of(user));
                when(authAccountRepository.existsByUserIdAndProvider(
                                anyLong(), eq(Provider.GENERAL)))
                        .thenReturn(true);
                when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
                when(authAccountRepository.findByUserIdAndProvider(anyLong(), eq(Provider.GENERAL)))
                        .thenReturn(Optional.of(authAccount));
                when(userStatusValidator.isWithinWithdrawGracePeriod(user)).thenReturn(true);
                when(tokenIssueService.issueRecoveryToken(
                                eq(user.getId()),
                                eq("inactive@example.com"),
                                eq(Provider.GENERAL.name()),
                                eq(Role.USER)))
                        .thenReturn("recoveryToken");

                // when
                UserResponseDTO.LoginResultDTO result = userService.loginUser(request);

                // then
                assertEquals("recoveryToken", result.getAccessToken());
                assertNull(result.getRefreshToken());
                assertEquals(UserStatus.INACTIVE, result.getStatus());
                verify(tokenIssueService, never())
                        .issueTokenPair(any(), any(), any(), any(), any(), any());
                verify(userStatusValidator, never()).validateLoginAllowed(any());
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("실패 - 탈퇴 유예 기간이어도 비밀번호가 틀리면 로그인에 실패한다")
            void 유예_기간이어도_비밀번호가_틀리면_로그인에_실패한다() {
                // given
                UserRequestDTO.LoginRequestDTO request =
                        new UserRequestDTO.LoginRequestDTO(
                                "inactive@example.com",
                                "wrongPassword",
                                "ios-iphone-16-pro",
                                DeviceType.PHONE);

                Users user =
                        Users.builder()
                                .id(2L)
                                .role(Role.USER)
                                .status(UserStatus.INACTIVE)
                                .password("encodedPassword")
                                .build();
                user.withdraw("테스트 탈퇴", java.time.LocalDateTime.now().minusDays(3));

                when(authAccountRepository.findUserByEmailAndProvider(
                                eq(request.email()), eq(Provider.GENERAL)))
                        .thenReturn(Optional.of(user));
                when(authAccountRepository.existsByUserIdAndProvider(
                                anyLong(), eq(Provider.GENERAL)))
                        .thenReturn(true);
                when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

                // when & then
                com.umc.linkyou.apiPayload.exception.handler.UserHandler ex =
                        assertThrows(
                                com.umc.linkyou.apiPayload.exception.handler.UserHandler.class,
                                () -> userService.loginUser(request));
                assertEquals(
                        com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus._LOGIN_FAILED,
                        ex.getCode());
                verify(userStatusValidator, never()).isWithinWithdrawGracePeriod(any());
                verify(tokenIssueService, never())
                        .issueRecoveryToken(any(), any(), any(), any());
            }

            @Test
            @DisplayName("실패 - 탈퇴 유예 기간이 지난 유저는 로그인이 차단된다")
            void 유예_기간이_지난_유저는_로그인이_차단된다() {
                // given
                UserRequestDTO.LoginRequestDTO request =
                        new UserRequestDTO.LoginRequestDTO(
                                "inactive@example.com",
                                "password123",
                                "ios-iphone-16-pro",
                                DeviceType.PHONE);

                Users user =
                        Users.builder()
                                .id(2L)
                                .role(Role.USER)
                                .status(UserStatus.INACTIVE)
                                .password("encodedPassword")
                                .build();
                user.withdraw("테스트 탈퇴", java.time.LocalDateTime.now().minusDays(20));

                AuthAccount authAccount =
                        AuthAccount.builder().user(user).email("inactive@example.com").build();

                when(authAccountRepository.findUserByEmailAndProvider(
                                eq(request.email()), eq(Provider.GENERAL)))
                        .thenReturn(Optional.of(user));
                when(authAccountRepository.existsByUserIdAndProvider(
                                anyLong(), eq(Provider.GENERAL)))
                        .thenReturn(true);
                when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
                when(authAccountRepository.findByUserIdAndProvider(anyLong(), eq(Provider.GENERAL)))
                        .thenReturn(Optional.of(authAccount));
                when(userStatusValidator.isWithinWithdrawGracePeriod(user)).thenReturn(false);
                doThrow(
                                new com.umc.linkyou.apiPayload.exception.GeneralException(
                                        com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus
                                                ._USER_INACTIVE))
                        .when(userStatusValidator)
                        .validateLoginAllowed(user);

                // when & then
                com.umc.linkyou.apiPayload.exception.GeneralException ex =
                        assertThrows(
                                com.umc.linkyou.apiPayload.exception.GeneralException.class,
                                () -> userService.loginUser(request));
                assertEquals(
                        com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus._USER_INACTIVE,
                        ex.getCode());
            }
        }
    }
}
