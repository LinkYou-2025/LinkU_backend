package com.umc.linkyou.oauth2;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
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
            if (name != null) {
                String normalizedInput = name.replaceAll("[^a-zA-Z0-9가-힣]", "").toLowerCase().trim();

                // 정규화된 이름이 현재 닉네임과 다를 때만 업데이트 시도
                if (!normalizedInput.isEmpty() && !normalizedInput.equals(user.getNickName())) {
                    // generateUniqueNickname에 현재 user 객체를 넘겨서 자기 자신은 중복에서 제외하게 함
                    String uniqueNickname = generateUniqueNickname(normalizedInput, email, user);
                    user.setNickName(uniqueNickname);
                    log.debug("기존 사용자 닉네임 동기화: userId={}, nickname={}", user.getId(), uniqueNickname);
                }
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

        String uniqueNickname = generateUniqueNickname(name, email, null);

        Users savedUser = userRepository.saveAndFlush(Users.builder()
                .password(passwordEncoder.encode("social_" + externalId.hashCode()))
                .nickName(uniqueNickname)
                .gender(null)
                .role(Role.USER)
                .status(UserStatus.TEMP)
                .build());


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
    private String generateUniqueNickname(String name, String email, Users currentUser) {
        String base = (name != null && !name.isBlank())
                ? name
                : (email != null && email.contains("@") ? email.substring(0, email.indexOf("@")) : "user");

        base = base.replaceAll("[^a-zA-Z0-9가-힣]", "").toLowerCase().trim();
        if (base.isBlank()) base = "user";

        // 1 & 2단계: 숫자 루프 (0~9) 및 랜덤 루프 (4회)
        for (int i = 0; i < 14; i++) {
            String candidate;
            if (i < 10) {
                candidate = (i == 0) ? base : base + "_" + i;
            } else {
                candidate = base + "_" + UUID.randomUUID().toString().substring(0, 5);
            }

            // 중복 체크 시: (전체 DB에 없거나) OR (현재 수정 중인 유저의 기존 닉네임과 같다면) 통과
            if (!userRepository.existsByNickName(candidate) ||
                    (currentUser != null && candidate.equals(currentUser.getNickName()))) {
                return candidate;
            }
        }

        // [3단계] 최종 단계: 8자리 랜덤값으로 딱 한 번 더 확인
        String finalCandidate = base + "_" + UUID.randomUUID().toString().substring(0, 8);

        // 만약 이 8자리 랜덤값마저 중복이라면 (이론상 가능하지만 확률은 거의 0)
        if (userRepository.existsByNickName(finalCandidate)) {
            log.error("닉네임 생성 실패: 모든 시도가 중복됨. base={}", base);
            // 무한 루프 대신 명확한 에러를 던져 트랜잭션을 중단시킵니다.
            throw new GeneralException(UserErrorStatus._DUPLICATE_NICKNAME);
        }

        return finalCandidate;
    }
}

