package com.umc.linkyou.service.users;


import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.jwt.JwtTokenProvider;
import com.umc.linkyou.jwt.RefreshTokenManager;
import com.umc.linkyou.jwt.TokenIssueService;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.enums.DeviceType;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.*;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.categoryRepository.UsersCategoryColorRepository;
import com.umc.linkyou.repository.classification.InterestRepository;
import com.umc.linkyou.repository.userRepository.UserQueryRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.JobRepository;
import com.umc.linkyou.repository.classification.PurposeRepository;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.jsonwebtoken.Claims;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    private final UserQueryRepository userQueryRepository;

    private final JwtTokenProvider jwtTokenProvider;

    private final JobRepository jobRepository;

    private final InterestRepository interestRepository;

    private final PurposeRepository purposeRepository;

    private final FolderRepository folderRepository;

    private final CategoryRepository categoryRepository;

    private final UsersFolderRepository usersFolderRepository;

    private final UsersCategoryColorRepository usersCategoryColorRepository;

    private final RefreshTokenManager refreshTokenManager;
    private final TokenIssueService tokenIssueService;
    private final UserStatusValidator userStatusValidator;

    private final AuthAccountRepository authAccountRepository;

    private final AlarmSettingRepository alarmSettingRepository;


    @Transactional
    public Users joinUser(UserRequestDTO.JoinDTO request) {

        log.info("=== gender 확인: {} ==={}", request.getGender(), request);
        // 1. 닉네임 중복 체크
        validateNickNameNotDuplicate(request.getNickName());

        // 2. 현재 시도하는 경로(GENERAL)로 이미 가입된 계정이 있는지 체크
        if (authAccountRepository.existsByProviderAndExternalId(Provider.GENERAL, request.getEmail())) {
            throw new UserHandler(UserErrorStatus._DUPLICATE_JOIN_REQUEST);
        }

        // 3. 기존 유저 통합 로직: 이메일로 가입된 다른 소셜 계정이 있는지 확인
        Users user = authAccountRepository.findUserByEmailAndProvider(request.getEmail(), Provider.GENERAL)
                .orElseGet(() -> {
                    // 3-1. 기존 유저가 아예 없으면 새로 생성
                    Job job = jobRepository.findById(request.getJobId())
                            .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));

                    Users newUser = UserConverter.toUser(request, job);
                    // 일반 로그인용 비밀번호 인코딩
                    newUser.encodePassword(passwordEncoder.encode(request.getPassword()));

                    // Purposes / Interests 설정
                    UserConverter.setupUserPurposes(newUser, request.getPurposeList());
                    UserConverter.setupUserInterests(newUser, request.getInterestList());

                    Users savedUser = userRepository.save(newUser);
                    setupUserAlarmSetting(savedUser);
                    return savedUser;
                });

        // 4. 기존 유저가 소셜 유저였다면, 일반 로그인용 비밀번호가 없을 수 있으므로 업데이트
        if (user.getPassword() == null || user.getPassword().startsWith("social_")) {
            user.encodePassword(passwordEncoder.encode(request.getPassword()));
        }

        // 5. 일반(GENERAL) 가입 정보(AuthAccount) 저장
        authAccountRepository.save(AuthAccount.builder()
                .user(user)
                .provider(Provider.GENERAL)
                .externalId(request.getEmail())
                .email(request.getEmail())
                .build());

        // 6. 상태 업데이트 및 초기 폴더 설정
        // 기존 유저가 있더라도 폴더가 없는 경우(TEMP 상태 등)를 대비해 체크 후 초기화
        if (user.getStatus() == UserStatus.TEMP || user.getUsersFoldersList().isEmpty()) {
            initUserFolders(user);
            user.setStatus(UserStatus.ACTIVE);
        }

        return user;
    }


    //일반 회원 로그인
    @Transactional
    public UserResponseDTO.LoginResultDTO loginUser(UserRequestDTO.LoginRequestDTO request) {
        Users user = authAccountRepository.findUserByEmailAndProvider(request.email(), Provider.GENERAL)
                .orElseThrow(() -> new UserHandler(UserErrorStatus._LOGIN_FAILED));

        Long userId = user.getId();
        userStatusValidator.validateLoginAllowed(user);
        // 소셜 전용 계정 차단 (GENERAL AuthAccount 없음)
        boolean hasGeneralAccount = authAccountRepository.existsByUserIdAndProvider(
                user.getId(), Provider.GENERAL);
        if (!hasGeneralAccount) {
            throw new UserHandler(ErrorStatus._SOCIAL_ACCOUNT_ONLY);
        }

        // social login은 password null, jwt login은 password null이면 error(NPE 방지 코드)
        if (user.getPassword() == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new UserHandler(UserErrorStatus._LOGIN_FAILED);
        }

        String email = authAccountRepository.findByUserIdAndProvider(userId, Provider.GENERAL)
                .map(AuthAccount::getEmail)
                .orElseThrow(() -> new UserHandler(UserErrorStatus._USER_NOT_FOUND));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, null,
                Collections.singleton(() -> user.getRole().name())
        );


        TokenIssueService.IssuedTokenPair tokenPair = tokenIssueService.issueTokenPair(
                user.getId(),
                email,
                Provider.GENERAL.name(),
                user.getRole(),
                request.deviceId(),
                request.deviceType()
        );
        return UserConverter.toLoginResultDTO(user, tokenPair.accessToken(), tokenPair.refreshToken());
    }

    @Transactional
    public Users socialCompleteProfile(Long userId, UserRequestDTO.SocialCompleteDTO request) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorStatus._USER_NOT_FOUND));

        if (user.getStatus() != UserStatus.TEMP) {
            throw new UserHandler(ErrorStatus._ALREADY_ACTIVE_USER);
        }
        // 2. 닉네임 중복 체크
        validateNickNameNotDuplicate(request.getNickName());

        // 3. 필수 정보 업데이트
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        user.setNickName(request.getNickName());
        user.setGender(UserConverter.toGender(request.getGender())); // 깔끔해짐
        user.setJob(job);


        // Purposes / Interests 설정
        UserConverter.setupUserPurposes(user, request.getPurposeList());
        UserConverter.setupUserInterests(user, request.getInterestList());

        // 알림 설정
        setupUserAlarmSetting(user);

        // 5. 상태 변경 → ACTIVE
        user.setStatus(UserStatus.ACTIVE);

        // 6. 저장 + 초기 폴더 생성
        Users savedUser = userRepository.save(user);
        initUserFolders(savedUser);

        return savedUser;
    }


    public UserResponseDTO.TokenPair reissueRefreshToken(UserRequestDTO.TokenReissueRequestDTO request) {
        if (request.refreshToken() == null || request.refreshToken().isBlank()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }


        String raw = jwtTokenProvider.normalizeStrict(request.refreshToken());

        // 1) 서명/만료 검증
        jwtTokenProvider.validateRefreshToken(raw, request.deviceId());

        // 2) 이메일 파싱
        Claims claims = jwtTokenProvider.validateAndParseRefresh(raw).getBody();
        String email = claims.getSubject();
        String providerStr = claims.get("provider", String.class);  // String 그대로!

        Users user = authAccountRepository.findUserByEmailAndProvider(
                email, Provider.valueOf(providerStr)
        ).orElseThrow(() -> new UserHandler(UserErrorStatus._USER_NOT_FOUND));

        Long userId = user.getId();

        // 3) INACTIVE 사용자 차단
        userStatusValidator.validateLoginAllowed(user);

        String oldId = jwtTokenProvider.hmac(raw);

        DeviceType deviceType = refreshTokenManager.consumeToken(userId, providerStr, request.deviceId(), oldId);

        // 4) 새 토큰 발급 및 저장
        TokenIssueService.IssuedTokenPair tokenPair = tokenIssueService.issueTokenPair(
                userId,
                email,
                providerStr,
                user.getRole(),
                request.deviceId(),
                deviceType
        );
        return new UserResponseDTO.TokenPair(tokenPair.accessToken(), tokenPair.refreshToken());
    }

    public void checkNicknameAvailable(String nickname) {
        validateNickNameNotDuplicate(nickname);
    }

    // 마이페이지 조회
    @Transactional
    public UserResponseDTO.UserProfileSummaryDto userInfo(Long userId, String loginProvider) {
        UserResponseDTO.UserProfileSummaryDto s = userQueryRepository.findUserProfileSummary(userId);
        String currentEmail = authAccountRepository.findEmailByUserIdAndProvider(userId, Provider.valueOf(loginProvider))
                .orElseThrow(() -> new UserHandler(UserErrorStatus._USER_NOT_FOUND));

        List<String> purposes = purposeRepository.findAllPurposeNamesByUserId(userId);
        List<String> interests = interestRepository.findAllInterestNamesByUserId(userId);

        return UserConverter.toUserInfoDTO(s, currentEmail, purposes, interests, loginProvider);
    }


    // 마이페이지 수정
    @Transactional
    public void updateUserProfile(Long userId, UserRequestDTO.UpdateProfileDTO request) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorStatus._USER_NOT_FOUND));

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        user.setJob(job);

        if (request.getNickname() != null && !request.getNickname().equals(user.getNickName())) {
            validateNickNameNotDuplicate(request.getNickname());
            user.setNickName(request.getNickname());
        }

        UserConverter.setupUserPurposes(user, request.getPurposes());
        UserConverter.setupUserInterests(user, request.getInterests());

        userRepository.save(user);
    }

    /* 공통 메서드 */
    // 알림 설정
    private void setupUserAlarmSetting(Users user) {
        // 기본 알림 설정 생성
        AlarmSetting defaultSetting = AlarmSetting.createDefault(user);

        // 알람 설정 저장
        alarmSettingRepository.save(defaultSetting);

    }

    private void validateNickNameNotDuplicate(String nickname) {
        if (userRepository.findByNickName(nickname).isPresent()) {
            throw new UserHandler(UserErrorStatus._DUPLICATE_NICKNAME);
        }
    }

    // 초기 폴더 생성 메서드 (color_code 에러 방지 반영)
    private void initUserFolders(Users user) {
        List<Category> categories = categoryRepository.findAll();
        List<UsersCategoryColor> userColors = new ArrayList<>();

        for (Category category : categories) {
            // 중분류 폴더 생성
            Folder subFolder = folderRepository.save(Folder.builder()
                    .folderName(category.getCategoryName())
                    .category(category)
                    .parentFolder(null)
                    .build());

            // 기본 카테고리 색상 설정
            Fcolor defaultColor = category.getFcolor();
            userColors.add(UsersCategoryColor.builder()
                    .user(user)
                    .category(category)
                    .fcolor(defaultColor)
                    .build());

            // UsersFolder 매핑
            usersFolderRepository.save(UsersFolder.builder()
                    .user(user)
                    .folder(subFolder)
                    .permissionType(PermissionType.OWNER)
                    .isBookmarked(false)
                    .build());
        }
        usersCategoryColorRepository.saveAll(userColors);
    }

    // 로그아웃
    public void logoutUser(Long userId) {
        // 1) 유저 존재 여부 확인
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(UserErrorStatus._USER_NOT_FOUND));

        // 2) 로그인한 모든 디바이스의 리프레시 토큰 삭제 (강제 로그아웃)
        refreshTokenManager.deleteAllTokens(userId);
    }
}
