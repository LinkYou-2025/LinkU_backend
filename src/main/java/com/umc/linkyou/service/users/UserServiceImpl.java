package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.config.security.jwt.JwtTokenProvider;
import com.umc.linkyou.converter.UserConverter;
import com.umc.linkyou.domain.EmailVerification;
import com.umc.linkyou.domain.UserRefreshToken;
import com.umc.linkyou.domain.enums.Gender;
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
import com.umc.linkyou.repository.categoryRepository.UsersCategoryColorRepository;
import com.umc.linkyou.repository.classification.InterestRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.JobRepository;
import com.umc.linkyou.repository.classification.PurposeRepository;
import com.umc.linkyou.service.EmailService;
import com.umc.linkyou.web.dto.EmailVerificationResponse;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.UserResponseDTO;
//import io.swagger.v3.oas.annotations.servers.Server;
import io.jsonwebtoken.Claims;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private static final String AUTH_CODE_PREFIX = "AuthCode ";

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final JwtTokenProvider jwtTokenProvider;

    private final EmailService emailService;

    private final EmailRepository emailRepository;

    private final JobRepository jobRepository;

    private final UserQueryRepository userQueryRepository;

    private final InterestRepository interestRepository;

    private final PurposeRepository purposeRepository;

    private final FolderRepository folderRepository;

    private final CategoryRepository categoryRepository;

    private final UsersFolderRepository usersFolderRepository;

    private final UsersCategoryColorRepository usersCategoryColorRepository;

    private final UserRefreshTokenRepository userRefreshTokenRepository;


    @Value("${jwt.token.expiration.refresh}")
    private long refreshTtlMs;

    @Value("${jwt.hmac-secret}")
    private String hmacSecret;

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    private String key(String id){ return "refreshToken:" + id; }

    private void logKey(String id){
        String k = key(id);
        Long pttl = stringRedisTemplate.getExpire(k, java.util.concurrent.TimeUnit.MILLISECONDS);
        Map<Object,Object> map = stringRedisTemplate.opsForHash().entries(k);
        log.debug("[redis] key={}, pttl(ms)={}, fields={}", k, pttl, map.keySet());
    }

    @Override
    @Transactional
    public Users joinUser(UserRequestDTO.JoinDTO request){
        if (userRepository.findByNickName(request.getNickName()).isPresent()) {
            throw new UserHandler(ErrorStatus._DUPLICATE_NICKNAME);
        }

        if(userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new UserHandler(ErrorStatus._DUPLICATE_JOIN_REQUEST);
        }
        // Job 엔티티를 DB에서 조회
        Job job = jobRepository.findById(request.getJobId())
                .orElseThrow(() -> new GeneralException(ErrorStatus._BAD_REQUEST));

        Users newUser = UserConverter.toUser(request,job);
        newUser.encodePassword(passwordEncoder.encode(request.getPassword()));

        List<String> purposeNames = request.getPurposeList(); // 프론트에서 받은 enum 이름 리스트

        final Users savedUser = newUser;

        List<Purposes> purposeList = purposeNames.stream()
                .map(name -> {
                    String purpose = name;
                    return new Purposes(purpose, savedUser);
                })
                .toList();

        List<String> interestNames = request.getInterestList(); // 프론트에서 받은 enum 이름 리스트

        List<Interests> interestList = interestNames.stream()
                .map(name -> {
                    String interest = name; // 문자열 → enum
                    return new Interests(interest, savedUser);
                })
                .toList();

        newUser.setPurposes(purposeList);
        newUser.setInterests(interestList);

        //return userRepository.save(newUser);
        newUser = userRepository.save(newUser);

        // 중분류 폴더 생성
        List<Category> categories = categoryRepository.findAll();
        List<UsersCategoryColor> userColors = new ArrayList<>();

        for (Category category : categories) {
            Folder subFolder = folderRepository.save(Folder.builder()
                    .folderName(category.getCategoryName())
                    .category(category)
                    .parentFolder(null)
                    .build());

            // 기본 카테고리 색상으로 설정
            Fcolor defaultColor = category.getFcolor();
            userColors.add(UsersCategoryColor.builder()
                    .user(newUser)
                    .category(category)
                    .fcolor(defaultColor)
                    .build());

            // UsersFolder 매핑
            usersFolderRepository.save(UsersFolder.builder()
                    .user(newUser)
                    .folder(subFolder)
                    .isOwner(true)
                    .isWriter(true)
                    .isViewer(true)
                    .isBookmarked(false)
                    .build());
        }

        usersCategoryColorRepository.saveAll(userColors);

        return newUser;
    }

    @Override
    @Transactional
    public UserResponseDTO.LoginResultDTO loginUser(UserRequestDTO.LoginRequestDTO request) {
        Users user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(()-> new UserHandler(ErrorStatus._LOGIN_FAILED));

        if(!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new UserHandler(ErrorStatus._LOGIN_FAILED);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                Collections.singleton(() -> user.getRole().name())
        );

        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getEmail());

        // 리프레시 토큰이 이미 있으면 토큰을 갱신하고 없으면 토큰을 추가
        userRefreshTokenRepository.findByUserId(user.getId())
                .ifPresent(t -> userRefreshTokenRepository.deleteById(t.getRefreshToken()));
        String id = hmac(jwtTokenProvider.normalizeStrict(refreshToken));
        userRefreshTokenRepository.save(new UserRefreshToken(id, user.getId(), refreshTtlMs));

        return UserConverter.toLoginResultDTO(user, accessToken, refreshToken);
    }

    private String hmac(String token) {
        try {
            var mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(hmacSecret.getBytes(), "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(token.getBytes()));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public UserResponseDTO.TokenPair reissueRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GeneralException(ErrorStatus._BAD_REQUEST);
        }

        String raw = jwtTokenProvider.normalizeStrict(refreshToken);

        // 1) 서명/만료 검증
        jwtTokenProvider.validateRefreshToken(raw);

        // 2) 이메일 파싱(액세스 토큰 재발급용)
        jwtTokenProvider.validateRefreshToken(raw);
        Claims claims = jwtTokenProvider.validateAndParseRefresh(refreshToken).getBody();
        String email = claims.getSubject();

        // 3) 화이트리스트 확인
        String oldId = hmac(raw);
        var saved = userRefreshTokenRepository.findById(oldId)
                .orElseThrow(() -> new UserHandler(ErrorStatus._UNAUTHORIZED));

        // 4) (권장) 로테이션: 이전 토큰 삭제 → 새 토큰 저장(※ DB 접근 없음)
        String newRefresh = jwtTokenProvider.createRefreshToken(email);
        String newId = hmac(jwtTokenProvider.normalizeStrict(newRefresh));
        userRefreshTokenRepository.deleteById(oldId);
        userRefreshTokenRepository.save(new UserRefreshToken(newId, saved.getUserId(), refreshTtlMs));

        // 5) 새 Access 발급
        String newAccess = jwtTokenProvider.createAccessToken(email);
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

        log.info("인증 코드: {}", authCode);

        try {
            emailService.sendEmail(toEmail, title, authCode);
            emailService.saveCode(toEmail, authCode);
            //redisService.setValues(AUTH_CODE_PREFIX + toEmail,
            //        authCode, Duration.ofMillis(this.authCodeExpirationMillis));
            log.info("이메일 전송 완료: {}", toEmail);
        } catch (Exception e) {
            log.error("이메일 전송 실패: {}", toEmail, e);
            throw e; // 혹은 적절한 커스텀 예외를 던짐
        }
    }

    private void checkDuplicatedEmail(String email) {
        Optional<Users> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            log.debug("MemberServiceImpl.checkDuplicatedEmail exception occur email: {}", email);
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

        return EmailVerificationResponse.of(isMatch);
    }

    // 마이페이지 조회
    @Override
    public UserResponseDTO.UserInfoDTO userInfo(Long userId){
        String nickName = userQueryRepository.findNicknameByUserId(userId);
        String email = userQueryRepository.findEmailByUserId(userId);
        Gender gender = userQueryRepository.findGenderByUserId(userId);
        Job job = userQueryRepository.findJobByUserId(userId);
        Long linkCount = userQueryRepository.countLinksByUserId(userId);
        Long folderCount = userQueryRepository.countFoldersByUserId(userId);
        Long aiLinkCount = userQueryRepository.countAiLinksByUserId(userId);

        return UserConverter.toUserInfoDTO(
                nickName, email, gender, job, linkCount, folderCount, aiLinkCount
        );
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

        // 닉네임 업데이트
        if (request.getNickname() != null) {
            user.setNickName(request.getNickname());
        }

        purposeRepository.deleteAllByUser(user); // 기존 목적 삭제

        List<Purposes> newPurposes = request.getPurposes().stream()
                .map(purpose -> new Purposes(purpose, user))
                .collect(Collectors.toList());
        purposeRepository.saveAll(newPurposes);

        interestRepository.deleteAllByUser(user); // 기존 관심사 삭제

        List<Interests> newInterests = request.getInterests().stream()
                .map(interest -> new Interests(interest, user))
                .collect(Collectors.toList());
        interestRepository.saveAll(newInterests);

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
        // 회원의 이메일인지 확인
        Optional <Users> users = userRepository.findByEmail(toEmail);
        if(users.isPresent()) {
            String title = "Link You 임시 비밀번호";
            // 임시 비밀번호 생성
            String tempPassword = this.createPassword();

            log.info("인증 코드: {}", tempPassword);

            try {
                emailService.sendEmail(toEmail, title, tempPassword);
                emailService.savePassword(toEmail, tempPassword);
                log.info("이메일 전송 완료: {}", toEmail);
            } catch (Exception e) {
                log.error("이메일 전송 실패: {}", toEmail, e);
                throw e;
            }
        }else {
            // 가입되지 않은 이메일인 경우
            throw new UserHandler(ErrorStatus._USER_NOT_FOUND);
        }
    }

    @Override
    @Transactional
    public Users withdrawUser(Long userId,UserRequestDTO.DeleteReasonDTO deleteReasonDTO) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        user.setStatus("INACTIVE");
        user.setInactiveDate(LocalDateTime.now());
        user.setDeleted_reason(deleteReasonDTO.getReason());
        userRepository.save(user);
        return user;
    }
    // 매일 새벽 3시 실행
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void deleteCompletelyInactiveUsers() {
        LocalDateTime tenDaysAgo = LocalDateTime.now().minusDays(10);
        List<Users> toDelete = userRepository.findAllByStatusAndInactiveDateBefore("INACTIVE", tenDaysAgo);
        if (!toDelete.isEmpty()) {
            // 삭제 대상 사용자 정보(예: id, email, nickName) 로그 문자열 생성
            String infoList = toDelete.stream()
                    .map(u -> String.format("id=%d, email=%s, nickName=%s", u.getId(), u.getEmail(), u.getNickName()))
                    .collect(Collectors.joining("; "));

            userRepository.deleteAll(toDelete);

            log.info("탈퇴 후 10일 경과 {}명 완전삭제 완료, 삭제유저: [{}]", toDelete.size(), infoList);
        }
    }

}

