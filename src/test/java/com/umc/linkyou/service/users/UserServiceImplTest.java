package com.umc.linkyou.service.users;

import com.umc.linkyou.config.properties.JwtProperties;
import com.umc.linkyou.jwt.AccessTokenBlackListManager;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.RefreshTokenManager;
import com.umc.linkyou.jwt.TokenIssueService;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.categoryRepository.UsersCategoryColorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.InterestRepository;
import com.umc.linkyou.repository.classification.JobRepository;
import com.umc.linkyou.repository.classification.PurposeRepository;
import com.umc.linkyou.repository.userRepository.UserQueryRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @InjectMocks
    private UserService userService;

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserQueryRepository userQueryRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private JobRepository jobRepository;
    @Mock private InterestRepository interestRepository;
    @Mock private PurposeRepository purposeRepository;
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
                UserRequestDTO.JoinDTO request = UserRequestDTO.JoinDTO.builder()
                        .nickName("테스터")
                        .email("test@example.com")
                        .password("password123")
                        .jobId(1L)
                        .gender(1)
                        .purposeList(new ArrayList<>())
                        .interestList(new ArrayList<>())
                        .termsMap(Collections.emptyMap())
                        .build();

                Job mockJob = Job.builder().id(1L).build();

                when(userRepository.findByNickName(eq(request.nickName()))).thenReturn(Optional.empty());
                when(jobRepository.findById(anyLong())).thenReturn(Optional.of(mockJob));
                when(authAccountRepository.existsByProviderAndExternalId(eq(Provider.GENERAL), anyString())).thenReturn(false);
                when(authAccountRepository.findUserByEmailAndProvider(anyString(), eq(Provider.GENERAL))).thenReturn(Optional.empty());
                when(userRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
                when(categoryRepository.findAll()).thenReturn(new ArrayList<>());
                when(usersCategoryColorRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

                // when
                Users savedUser = userService.joinUser(request);

                // then
                assertNotNull(savedUser.getRole());
                assertEquals(Role.USER, savedUser.getRole());
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
            @DisplayName("성공 - TEMP 상태 유저가 프로필 완성 시 ACTIVE 상태로 변경된다")
            void social_complete_success() {
                // given
                Users tempUser = Users.builder()
                        .id(1L)
                        .status(UserStatus.TEMP)
                        .usersFoldersList(new ArrayList<>())
                        .build();

                UserRequestDTO.SocialCompleteDTO request = new UserRequestDTO.SocialCompleteDTO(
                        "완성닉네임", 1, 1L, new ArrayList<>(), new ArrayList<>(), Collections.emptyMap()
                );

                when(userRepository.findById(eq(tempUser.getId()))).thenReturn(Optional.of(tempUser));
                when(userRepository.findByNickName(eq("완성닉네임"))).thenReturn(Optional.empty());
                when(jobRepository.findById(anyLong())).thenReturn(Optional.of(Job.builder().id(1L).build()));
                when(userRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));
                when(categoryRepository.findAll()).thenReturn(new ArrayList<>());
                when(usersCategoryColorRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

                // when
                Users result = userService.socialCompleteProfile(tempUser.getId(), request);

                // then
                assertEquals(UserStatus.ACTIVE, result.getStatus());
                assertEquals("완성닉네임", result.getNickName());
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
                UserRequestDTO.LoginRequestDTO request = new UserRequestDTO.LoginRequestDTO(
                        "test@example.com",
                        "password123",
                        "ios-iphone-16-pro",
                        DeviceType.PHONE
                );

                Users user = Users.builder()
                        .id(1L)
                        .role(Role.USER)
                        .status(UserStatus.ACTIVE)
                        .password("encodedPassword")
                        .build();

                AuthAccount authAccount = AuthAccount.builder()
                        .user(user)
                        .email("test@example.com")
                        .build();

                when(authAccountRepository.findUserByEmailAndProvider(eq(request.email()), eq(Provider.GENERAL)))
                        .thenReturn(Optional.of(user));

                when(authAccountRepository.existsByUserIdAndProvider(anyLong(), eq(Provider.GENERAL)))
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
                        eq(request.deviceType())
                )).thenReturn(issuedTokenPair);

                // when
                UserResponseDTO.LoginResultDTO result = userService.loginUser(request);

                // then
                assertNotNull(result.getAccessToken());
                verify(userStatusValidator).validateLoginAllowed(user);
                verify(tokenIssueService).issueTokenPair(
                        eq(user.getId()),
                        eq("test@example.com"),
                        eq(Provider.GENERAL.name()),
                        eq(Role.USER),
                        eq(request.deviceId()),
                        eq(request.deviceType())
                );
            }
        }
    }
}