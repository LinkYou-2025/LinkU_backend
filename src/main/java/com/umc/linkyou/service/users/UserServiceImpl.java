package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.config.security.jwt.RefreshTokenManager;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.EmailVerification;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Interests;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.classification.Purposes;
import com.umc.linkyou.domain.Users;
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
import com.umc.linkyou.service.EmailService;
import com.umc.linkyou.web.dto.EmailVerificationResponse;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    private final UserQueryRepository userQueryRepository;

    private final JwtTokenProvider jwtTokenProvider;

    private final EmailService emailService;

    private final EmailRepository emailRepository;

    private final JobRepository jobRepository;

    private final InterestRepository interestRepository;

    private final PurposeRepository purposeRepository;

    private final FolderRepository folderRepository;

    private final CategoryRepository categoryRepository;

    private final UsersFolderRepository usersFolderRepository;

    private final UsersCategoryColorRepository usersCategoryColorRepository;

    private final RefreshTokenManager refreshTokenManager;

    private final AuthAccountRepository authAccountRepository;

    @Value("${jwt.token.expiration.refresh}")
    private long refreshTtlMs;

    @Value("${jwt.hmac-secret}")
    private String hmacSecret;


    @Override
    @Transactional
    public Users joinUser(UserRequestDTO.JoinDTO request) {

        log.info("=== gender 확인: {} ==={}", request.getGender(),request);
        // 1. 닉네임 중복 체크
        if (userRepository.findByNickName(request.getNickName()).isPresent()) {
            throw new UserHandler(ErrorStatus._DUPLICATE_NICKNAME);
        }

        // 2. 현재 시도하는 경로(GENERAL)로 이미 가입된 계정이 있는지 체크
        if (authAccountRepository.existsByProviderAndExternalId(Provider.GENERAL, request.getEmail())) {
            throw new UserHandler(ErrorStatus._DUPLICATE_JOIN_REQUEST);
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
                    setupUserPurposes(newUser, request.getPurposeList());
                    setupUserInterests(newUser, request.getInterestList());

                    return userRepository.save(newUser);
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

    // 중복 코드 방지를 위한 Purposes/Interests 설정 헬퍼 메서드
// 1. 목적(Purpose) 설정 전용
    private void setupUserPurposes(Users user, List<String> purposeNames) {
        // No-op: null인 경우 기존 데이터를 건드리지 않음
        if (purposeNames == null) {
            return;
        }

        // 1. 기존 컬렉션 비우기 (orphanRemoval = true에 의해 DB 삭제 예약)
        if (user.getPurposes() != null) {
            user.getPurposes().clear();
        } else {
            // null 방지 로직 (필요시)
            // user.setPurposes(new ArrayList<>());
        }

        // 2. 새로운 데이터 추가 (CascadeType.ALL에 의해 부모 저장 시 자동 저장)
        if (!purposeNames.isEmpty()) {
            purposeNames.stream()
                    .map(name -> new Purposes(name, user))
                    .forEach(purpose -> user.getPurposes().add(purpose));
        }
    }

    // 2. 관심사(Interest) 설정 전용
    private void setupUserInterests(Users user, List<String> interestNames) {
        // No-op: null인 경우 기존 데이터를 건드리지 않음
        if (interestNames == null) {
            return;
        }

        // 1. 기존 컬렉션 비우기
        if (user.getInterests() != null) {
            user.getInterests().clear();
        }

        // 2. 새로운 데이터 추가
        if (!interestNames.isEmpty()) {
            interestNames.stream()
                    .map(name -> new Interests(name, user))
                    .forEach(interest -> user.getInterests().add(interest));
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
    //일반 회원 로그인
    @Override
    @Transactional
    public UserResponseDTO.LoginResultDTO loginUser(UserRequestDTO.LoginRequestDTO request) {
        Users user = authAccountRepository.findUserByEmailAndProvider(request.getEmail(), Provider.GENERAL)
                .orElseThrow(() -> new UserHandler(ErrorStatus._LOGIN_FAILED));

        Long userId = user.getId();

        // 소셜 전용 계정 차단 (GENERAL AuthAccount 없음)
        boolean hasGeneralAccount = authAccountRepository.existsByUserIdAndProvider(
                user.getId(), Provider.GENERAL);
        if (!hasGeneralAccount) {
            throw new UserHandler(ErrorStatus._SOCIAL_ACCOUNT_ONLY);
        }

        // social login은 password null, jwt login은 password null이면 error(NPE 방지 코드)
        if (user.getPassword() == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserHandler(ErrorStatus._LOGIN_FAILED);
        }

        String email = authAccountRepository.findByUserIdAndProvider(userId, Provider.GENERAL)
                .map(AuthAccount::getEmail)
                .orElseThrow(() -> new UserHandler(ErrorStatus._USER_NOT_FOUND));

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                email, null,
                Collections.singleton(() -> user.getRole().name())
        );



        String accessToken = jwtTokenProvider.createAccessToken(email, Provider.GENERAL.name());
        String refreshToken = jwtTokenProvider.createRefreshToken(email);

        // 리프레시 토큰이 이미 있으면 토큰을 갱신하고 없으면 토큰을 추가
        String tokenId =  jwtTokenProvider.hmac(jwtTokenProvider.normalizeStrict(refreshToken));
        refreshTokenManager.saveToken(user.getId(), tokenId, Provider.GENERAL.name(), refreshTtlMs);
        return UserConverter.toLoginResultDTO(user, accessToken, refreshToken);
    }

    @Override
    @Transactional
    public Users socialCompleteProfile(Users user, UserRequestDTO.SocialCompleteDTO request) {
        // 1. 이미 ACTIVE 상태인 유저라면 에러 발생 (또는 바로 유저 반환)
        if (user.getStatus() == UserStatus.ACTIVE) {
            // "이미 프로필 설정이 완료된 사용자입니다"라는 에러를 던집니다.
            throw new UserHandler(ErrorStatus._ALREADY_ACTIVE_USER);
        }
        // 2. 닉네임 중복 체크
        validateNickNameNotDuplicate(request.getNickName());

        // 3. 필수 정보 업데이트
        user.setNickName(request.getNickName());

        Integer genderCode = request.getGender();
        if (genderCode == null || (genderCode != 1 && genderCode != 2)) {
            throw new GeneralException(ErrorStatus._INVALID_GENDER);
        }

        Gender gender = switch (genderCode) {
            case 1 -> Gender.MALE;
            case 2 -> Gender.FEMALE;
            default -> throw new GeneralException(ErrorStatus._INVALID_GENDER);
        };
        user.setGender(gender);


        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        user.setJob(job);


        // Purposes / Interests 설정
        setupUserPurposes(user, request.getPurposeList());
        setupUserInterests(user, request.getInterestList());

        // 5. 상태 변경 → ACTIVE
        user.setStatus(UserStatus.ACTIVE);

        // 6. 저장 + 초기 폴더 생성
        Users savedUser = userRepository.save(user);
        initUserFolders(savedUser);  // 기존 joinUser 로직 추출

        return savedUser;
    }


    public UserResponseDTO.TokenPair reissueRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        String raw = jwtTokenProvider.normalizeStrict(refreshToken);

        // 1) 서명/만료 검증
        jwtTokenProvider.validateRefreshToken(raw);

        // 2) 이메일 파싱
        Claims claims = jwtTokenProvider.validateAndParseRefresh(raw).getBody();
        String email = claims.getSubject();
        String providerStr = claims.get("provider", String.class);  // String 그대로!

        Long userId = authAccountRepository.findUserByEmailAndProvider(email,Provider.valueOf(providerStr))
                .map(Users::getId)
                .orElseThrow(() -> new UserHandler(ErrorStatus._USER_NOT_FOUND));

        String oldId = jwtTokenProvider.hmac(raw);

        // 3) provider 조회
        String provider = refreshTokenManager.consumeToken(userId, oldId)  // 여기서 provider 반환
                .orElseThrow(() -> new UserHandler(ErrorStatus._INVALID_TOKEN));

        // 4) 새 토큰 발급 및 저장
        String newRefresh = jwtTokenProvider.createRefreshToken(email);
        String newId = jwtTokenProvider.hmac(jwtTokenProvider.normalizeStrict(newRefresh));
        refreshTokenManager.saveToken(userId, newId, provider, refreshTtlMs);

        String newAccess = jwtTokenProvider.createAccessToken(email, provider);
        return new UserResponseDTO.TokenPair(newAccess, newRefresh);
    }

    @Override
    public void validateNickNameNotDuplicate(String nickname) {
        if (userRepository.findByNickName(nickname).isPresent()) {
            throw new UserHandler(ErrorStatus._DUPLICATE_NICKNAME);
        }
    }

    // 이메일 인증
    // 인증 코드 전송
    public void sendCode(String toEmail) {
        this.checkDuplicatedEmail(toEmail);
        String title = "Link You 이메일 인증 번호";
        String authCode = this.createCode();
        int expiresInMinutes = 10;
        String nickname = "링큐 회원";

        log.info("인증 코드: {}", authCode);

        try {
            //emailService.sendEmail(toEmail, title, authCode);
            //emailService.saveCode(toEmail, authCode);

            // 템플릿 기반 HTML 메일로 전송
            emailService.sendVerificationEmailTemplate(
                    toEmail,
                    nickname,
                    authCode,
                    expiresInMinutes
            );

            emailService.saveCode(toEmail, authCode);
            log.info("이메일 전송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("이메일 전송 실패: {}", toEmail, e);
            throw e; // 혹은 적절한 커스텀 예외를 던짐
        }
    }

    private void checkDuplicatedEmail(String email) {
        if (authAccountRepository.existsByEmail(email)) {
            log.debug("checkDuplicatedEmail exception occur email: {}", email);
            throw new UserHandler(ErrorStatus._DUPLICATE_JOIN_REQUEST);
        }
    }

    // 인증 코드 생성
    private String createCode() {
        int lenth = 6;
        try {
            Random random = SecureRandom.getInstanceStrong();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < lenth; i++) {
                builder.append(random.nextInt(10));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            log.debug("MemberService.createCode() exception occur");
            throw new UserHandler(ErrorStatus._NO_SUCH_ALGORITHM);
        }
    }

    // 인증 코드 검증
    public EmailVerificationResponse verifyCode(String email, String authCode) {
        this.checkDuplicatedEmail(email);
        EmailVerification verification = emailRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorStatus._VERIFICATION_FAILED));

        // 만료 시간 체크
        if (verification.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UserHandler(ErrorStatus._EXPIRED_VERIFICATION_CODE);
        }

        // 코드 일치 여부 확인
        boolean isMatch = verification.getVerificationCode().equals(authCode);

        // 결과 반영
        if (isMatch) {
            verification.setIsVerified(true);
            emailRepository.save(verification);
        }

        else if (isMatch==false)
            throw new UserHandler(ErrorStatus._VERIFICATION_FAILED);

        return EmailVerificationResponse.of(isMatch);
    }

    // 마이페이지 조회
    @Override
    public UserResponseDTO.UserProfileSummaryDto userInfo(Long userId, String loginProvider) {
        UserResponseDTO.UserProfileSummaryDto s = userQueryRepository.findUserProfileSummary(userId);
        String currentEmail = authAccountRepository.findEmailByUserIdAndProvider(userId, Provider.valueOf(loginProvider))
                .orElseThrow(() -> new UserHandler(ErrorStatus._USER_NOT_FOUND));

        List<String> purposes  = purposeRepository.findAllPurposeNamesByUserId(userId);
        List<String> interests = interestRepository.findAllInterestNamesByUserId(userId);

        UserResponseDTO.UserProfileSummaryDto result = UserConverter.toUserInfoDTO(s, currentEmail, purposes, interests);
        result.setLoginProvider(loginProvider);
        return result;
    }


    // 마이페이지 수정
    @Override
    @Transactional
    public void updateUserProfile(Long userId, UserRequestDTO.UpdateProfileDTO request) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserHandler(ErrorStatus._USER_NOT_FOUND));

        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));
        user.setJob(job);

        if (request.getNickname() != null && !request.getNickname().equals(user.getNickName())) {
            boolean exists = userRepository.findByNickName(request.getNickname()).isPresent();
            if (exists) throw new UserHandler(ErrorStatus._DUPLICATE_NICKNAME);
            user.setNickName(request.getNickname());
        }

        setupUserPurposes(user, request.getPurposes());
        setupUserInterests(user, request.getInterests());

        userRepository.save(user);
    }


    // 임시 비밀번호 생성
    public String createPassword() {
        final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";

        final String NUMBERS = "0123456789";

        final String SPECIAL_CHAR = "!@#$%^&*()-_+=<>?";

        final String ALL_CHARS = UPPERCASE + LOWERCASE + NUMBERS + SPECIAL_CHAR;

        final int length = 8;

        // 난수 생성기 객체
        SecureRandom random = new SecureRandom();
        // 문자열 생성 객체
        StringBuilder sb = new StringBuilder();

        sb.append(getRandomChar(UPPERCASE, random));
        sb.append(getRandomChar(LOWERCASE, random));
        sb.append(getRandomChar(NUMBERS, random));
        sb.append(getRandomChar(SPECIAL_CHAR, random));

        // 나머지 글자 랜덤하게 채우기
        for(int i = 4; i < length; i++) {
            sb.append(getRandomChar(ALL_CHARS, random));
        }

        // 비밀번호를 랜덤하게 섞음
        return shuffleString(sb.toString(), random);
    }

    // 랜덤 문자 메서드
    private static String getRandomChar(String characters, SecureRandom random){
        return String.valueOf(characters.charAt(random.nextInt(characters.length())));
    }

    // 문자열 섞는 메서드
    private static String shuffleString(String input, SecureRandom random){
        char[] characters = input.toCharArray();
        for(int i = characters.length - 1; i >= 0; i--){
            int j = random.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        return new String(characters);
    }

    // 임시 비밀번호 전송
    @Override
    public void sendTempPassword(String toEmail) {
        Users user = authAccountRepository.findUserByEmailAndProvider(toEmail, Provider.GENERAL)
                .orElseThrow(() -> new UserHandler(ErrorStatus._USER_NOT_FOUND));

        String tempPassword = this.createPassword();
        int expiresInMinutes = 10; // 템플릿에서 표시용

        try {
            // 1) 임시 비밀번호 저장(암호화)
            emailService.savePassword(toEmail, tempPassword);

            // 2) 메일 발송 (템플릿)
            String nickname = (user.getNickName() == null || user.getNickName().isBlank())
                    ? "링큐 회원" : user.getNickName();

            emailService.sendTempPasswordTemplate(
                    toEmail,
                    nickname,
                    tempPassword,
                    expiresInMinutes
            );

            log.info("임시 비밀번호 메일 전송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("임시 비밀번호 발송 실패: {}", toEmail, e);
            throw e;
        }
    }


    @Override
    @Transactional
    public Users withdrawUser(Long userId,UserRequestDTO.DeleteReasonDTO deleteReasonDTO) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        user.setStatus(UserStatus.INACTIVE);
        user.setInactiveDate(LocalDateTime.now());
        user.setDeleted_reason(deleteReasonDTO.getReason());
        userRepository.save(user);
        return user;
    }
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteCompletelyInactiveUsers() {
        LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(10);
        List<Long> inactiveUserIds = userRepository.findInactiveUserIds(tenDaysAgo);

        if (inactiveUserIds.isEmpty()) {
            log.debug("삭제할 비활성 사용자 없음");
            return;
        }

        // 1. Redis 삭제
        for (Long userId : inactiveUserIds) {
            refreshTokenManager.deleteAllTokens(userId);
        }

        // 2. 엔티티 로드
        List<Users> toDelete = userRepository.findAllById(inactiveUserIds);

        // 3. 연관관계 수동 정리 (FK 제약 조건 방지)
        for (Users user : toDelete) {
            // 자식의 자식 (LinkuFolder)부터 순차 삭제 트리거
            user.getUsersLinkus().forEach(ul -> ul.getLinkuFolders().clear());

            // Users 엔티티에 연결된 모든 리스트 clear
            // orphanRemoval = true 설정에 의해 DB 삭제 쿼리가 예약됩니다.
            user.getUsersLinkus().clear();
            user.getUserAlarms().clear();
            user.getUserFcmTokens().clear();
            user.getCurations().clear();
            user.getCurationLikes().clear();
            user.getEmotionLogs().clear();
            user.getFolderShareLinks().clear();
            user.getRecentViewedLinkus().clear();
            user.getUsersFoldersList().clear();
            user.getUsersCategoryColorList().clear(); // 서버 에러 포인트 해결
            user.getPurposes().clear();
            user.getInterests().clear();
            user.getAuthAccounts().clear();
        }

        // 4. 부모 엔티티 삭제
        // deleteAllInBatch 대신 deleteAll을 사용하여 영속성 컨텍스트를 거쳐 안전하게 삭제합니다.
        userRepository.deleteAll(toDelete);

        log.info("🗑️ 비활성 사용자 {}명 및 모든 연관 데이터 완전삭제 완료", toDelete.size());
    }

    // 🔥 테스트 메서드 (단일 삭제용)
    @Transactional
    public void testImmediateDelete(Long userId) {
        if (userId == null) {
            log.info("testImmediateDelete: userId 없음");
            return;
        }

        // 1. Redis 삭제
        refreshTokenManager.deleteAllTokens(userId);
        log.debug("Redis 테스트삭제: userId={}", userId);

        // 2. 엔티티 로드 및 정리
        userRepository.findById(userId).ifPresent(user -> {
            // 깊은 관계부터 정리
            user.getUsersLinkus().forEach(ul -> ul.getLinkuFolders().clear());
            user.getUsersLinkus().clear();

            // 모든 컬렉션 clear
            user.getUserAlarms().clear();
            user.getUserFcmTokens().clear();
            user.getCurations().clear();
            user.getCurationLikes().clear();
            user.getEmotionLogs().clear();
            user.getFolderShareLinks().clear();
            user.getRecentViewedLinkus().clear();
            user.getUsersFoldersList().clear();
            user.getUsersCategoryColorList().clear();
            user.getPurposes().clear();
            user.getInterests().clear();
            user.getAuthAccounts().clear();

            // 3. 최종 삭제
            userRepository.delete(user);
            log.warn("🧪 테스트삭제 완료: userId={}", userId);
        });
    }



}

