package com.umc.linkyou.service.users;

import com.umc.linkyou.config.properties.JwtProperties;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
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

    // UserService가 의존하는 모든 필드를 Mock으로 선언합니다.
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
    @Mock private UserStatusValidator userStatusValidator;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private JwtProperties jwtProperties;
    @Mock private AlarmSettingRepository alarmSettingRepository;

    @Test
    @DisplayName("소셜 프로필 완성 시 유저 상태가 TEMP에서 ACTIVE로 변경된다.")
    void socialCompleteProfile_StatusChange() {
        // given
        Users tempUser = Users.builder()
                .id(1L)
                .status(UserStatus.TEMP)
                .usersFoldersList(new ArrayList<>())
                .build();

        UserRequestDTO.SocialCompleteDTO request = new UserRequestDTO.SocialCompleteDTO();
        request.setNickName("완성닉네임");
        request.setGender(1);
        request.setJobId(1L);
        request.setPurposeList(new ArrayList<>());
        request.setInterestList(new ArrayList<>());

        when(userRepository.findById(tempUser.getId())).thenReturn(Optional.of(tempUser));
        when(userRepository.findByNickName("완성닉네임")).thenReturn(Optional.empty());
        when(jobRepository.findById(anyLong())).thenReturn(Optional.of(Job.builder().id(1L).build()));
        when(userRepository.save(any(Users.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.findAll()).thenReturn(new ArrayList<>());
        when(usersCategoryColorRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        Users result = userService.socialCompleteProfile(tempUser.getId(), request);

        // then
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        assertEquals("완성닉네임", result.getNickName());
        verify(userRepository).findByNickName("완성닉네임");
    }

    @Test
    @DisplayName("일반 회원가입 시 유저에게 기본 ROLE_USER 권한이 부여된다.")
    void joinUser_CheckRoleAssignment() {
        // given
        UserRequestDTO.JoinDTO request = new UserRequestDTO.JoinDTO();
        request.setNickName("테스터");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setJobId(1L);
        request.setGender(1);
        request.setPurposeList(new ArrayList<>());
        request.setInterestList(new ArrayList<>());

        Job mockJob = Job.builder().id(1L).build();

        when(userRepository.findByNickName("테스터")).thenReturn(Optional.empty());
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
        verify(alarmSettingRepository).save(any());
    }

    @Test
    @DisplayName("로그인 성공 시 반환되는 DTO에 유저의 권한(Role) 정보가 포함된다.")
    void loginUser_CheckRoleInResponse() {
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

        when(authAccountRepository.findUserByEmailAndProvider(anyString(), eq(Provider.GENERAL))).thenReturn(Optional.of(user));
        when(authAccountRepository.existsByUserIdAndProvider(anyLong(), eq(Provider.GENERAL))).thenReturn(true);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(authAccountRepository.findByUserIdAndProvider(anyLong(), eq(Provider.GENERAL))).thenReturn(Optional.of(authAccount));

        TokenIssueService.IssuedTokenPair issuedTokenPair =
                new TokenIssueService.IssuedTokenPair("mockAccess", "mockRefresh");
        when(tokenIssueService.issueTokenPair(
                eq(user.getId()),
                eq("test@example.com"),
                eq(Provider.GENERAL.name()),
                eq(Role.USER),
                eq("ios-iphone-16-pro"),
                eq(DeviceType.PHONE)
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
                eq("ios-iphone-16-pro"),
                eq(DeviceType.PHONE)
        );
    }
}
