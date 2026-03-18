package com.umc.linkyou.oauth2;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional
public class UserSocialLoginHelper {

    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 웹 호출 시: socialToken 있음
     * 모바일 호출 시: socialToken null
     */
    public Users findOrCreateUser(String email, String name, String externalId,
                                  String profileImage, Provider provider, String socialToken) {

        validateRequiredIdentifiers(email, externalId, provider);
        Optional<AuthAccount> authAccountOpt =
                authAccountRepository.findByProviderAndExternalId(provider, externalId);
        /** 1. 재로그인: 소셜로그인 정보로 동기화하지만, 닉네임 유니크 보장 */
        if (authAccountOpt.isPresent()) {
            AuthAccount authAccount = authAccountOpt.get();
            Users user = authAccount.getUser();

            // 닉네임 무조건 동기화 (단, 값이 다를 때만 실행하여 쿼리 절약)
            if (name != null && !name.trim().equals(user.getNickName())) {
                String uniqueNickname = generateUniqueNickname(name.trim(), email);
                user.setNickName(uniqueNickname);
                log.debug("기존 사용자 닉네임 동기화: userId={}, nickname={}", user.getId(), uniqueNickname);
            }

            // 프로필 이미지 무조건 동기화
            if (profileImage != null && !profileImage.equals(authAccount.getProfileImage())) {
                authAccount.updateProfileImage(profileImage);
            }

            if (socialToken != null) authAccount.updateToken(socialToken);
            return user;
        }

        /** 2. 이메일이 있지만 다른 소셜로그인 계정 */
        Optional<Users> existingUserOpt = authAccountRepository
                .findUserByEmailExcludingProvider(email, provider);  //다른 provider만!
        if (existingUserOpt.isPresent()) {
            Users user = existingUserOpt.get();
            saveAuthAccount(user, provider, externalId, profileImage, socialToken, email);
            return user;
        }

        /** 3.완전 신규 사용자 + AuthAccount 일괄 생성*/
        return createNewUserWithAccount(email, name, provider, externalId, profileImage, socialToken);
    }

    /** 모바일용 (socialToken 없음) */
    public Users findOrCreateUser(String email, String name, String externalId,
                                  String profileImage, Provider provider) {
        return findOrCreateUser(email, name, externalId, profileImage, provider, null);
    }

    /** 3.완전 신규 사용자 + AuthAccount 일괄 생성*/
    private Users createNewUserWithAccount(String email, String name, Provider provider,
                                           String externalId, String profileImage, String socialToken) {

        String uniqueNickname = generateUniqueNickname(name, email);

        Users savedUser = userRepository.saveAndFlush(Users.builder()
                .password(passwordEncoder.encode("social_" + externalId.hashCode()))
                .nickName(uniqueNickname)
                .gender(null)
                .role(Role.USER)
                .status(UserStatus.TEMP)
                .build());

        log.info("신규 소셜 사용자 생성: id={}, nickname={}", savedUser.getId(), uniqueNickname);
        saveAuthAccount(savedUser, provider, externalId, profileImage, socialToken, email);
        return savedUser;
    }
    /** 2. 이메일이 있지만 다른 소셜로그인 계정 */
    private void saveAuthAccount(Users user, Provider provider, String externalId,
                                 String profileImage, String socialToken, String email) {
        try {
            authAccountRepository.save(AuthAccount.builder()
                    .user(user)
                    .email(email)
                    .provider(provider)
                    .externalId(externalId)
                    .profileImage(profileImage)
                    .socialToken(socialToken)
                    .build());
        } catch (Exception e) {
            log.error("AuthAccount 저장 실패: userId={}, provider={}", user.getId(), provider, e);
            throw new GeneralException(ErrorStatus._AUTH_ACCOUNT_SAVE_FAILED);
        }
    }
    private void validateRequiredIdentifiers(String email, String externalId, Provider provider) {
        if (email == null || email.trim().isEmpty()) {
            throw new GeneralException(ErrorStatus._SOCIAL_EMAIL_REQUIRED);
        }
        if (!email.contains("@")) {
            throw new GeneralException(ErrorStatus._INVALID_EMAIL_FORMAT);
        }
        if (externalId == null || externalId.trim().isEmpty()) {
            throw new GeneralException(ErrorStatus._SOCIAL_EXTERNAL_ID_REQUIRED);
        }
        if (provider == null) {
            throw new GeneralException(ErrorStatus._SOCIAL_UNSUPPORTED_PROVIDER);
        }
    }

    /**
     * 서비스 내 유니크한 닉네임을 생성하는 로직 (기존 로직 분리)
     */
    private String generateUniqueNickname(String name, String email) {
        // 1. 소셜로그인에서 입력받은 닉네임으로 생성 -> 이름이 없으면 이메일의 앞부분(아이디로) -> user
        String base = (name != null && !name.isBlank())
                ? name
                : (email != null && email.contains("@") ? email.substring(0, email.indexOf("@")) : "user");

        // 특수문자 제거 및 소문자화
        base = base.replaceAll("[^a-zA-Z0-9가-힣]", "").toLowerCase();
        if (base.isBlank()) base = "user";

        // 2. [1차 시도] 숫자 붙이기 (base, base_1 ~ base_9)
        for (int i = 0; i < 10; i++) {
            String candidate = (i == 0) ? base : base + "_" + i;
            if (!userRepository.existsByNickName(candidate)) {
                return candidate;
            }
        }

        // 3. [2차 시도] 10번 다 찼다면 랜덤 문자열 붙여서 중복 체크 (4회 루프)
        log.info("닉네임 숫자 중복 초과로 랜덤 루프 진입: base={}", base);
        for (int j = 0; j < 4; j++) {
            String randomSuffix = UUID.randomUUID().toString().substring(0, 5);
            String randomCandidate = base + "_" + randomSuffix;

            if (!userRepository.existsByNickName(randomCandidate)) {
                return randomCandidate;
            }
        }

        // 4. [최후의 수단] 4번의 랜덤 체크조차 실패했다면 (확률 극히 낮음) 그냥 마지막 생성값 반환
        return base + "_" + UUID.randomUUID().toString().substring(0, 8);
    }
}

