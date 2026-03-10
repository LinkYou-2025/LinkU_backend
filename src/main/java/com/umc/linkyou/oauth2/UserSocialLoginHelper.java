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
        /** 1. 재로그인: 닉네임/프로필/토큰 업데이트 */
        if (authAccountOpt.isPresent()) {
            AuthAccount authAccount = authAccountOpt.get();
            Users user = authAccount.getUser();

            // 닉네임 업데이트 (중복 아닐 때만)
            if (name != null) {
                String trimmedName = name.trim();
                if (!trimmedName.isEmpty()  // 공백만 있는 이름 차단
                        && !trimmedName.equals(user.getNickName())  // trim 후 비교
                        && !userRepository.existsByNickName(trimmedName)) {  // trim 저장
                    user.setNickName(trimmedName);  // 공백 제거된 이름 저장
                }
            }
            // 프로필 이미지 업데이트
            if (profileImage != null && !profileImage.equals(authAccount.getProfileImage())) {
                authAccount.updateProfileImage(profileImage);
            }
            // 웹 socialToken 업데이트 (모바일은 null이라 업데이트 안 됨)
            if (socialToken != null) {
                authAccount.updateToken(socialToken);
            }
            return user;
        }

        /** 2. 이메일이 있지만 다른 소셜로그인 계정 */
        Optional<Users> existingUserOpt = authAccountRepository.findUserByEmail(email);
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
        //닉네임 생성 전략: 소셜 이름 우선 사용 -> 이름 없으면 이메일 도메인
        String base = (name != null && !name.isBlank())
                ? name  // 소셜 이름 우선
                : (email.contains("@")
                    ? email.substring(0, email.indexOf("@"))
                : email)
                // "user@gmail.com" → "user"
                .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        if (base.isBlank()) base = "user";  // 안전장치

        Users savedUser = null;
        // 닉네임 중복 시 번호 붙이기
        for (int i = 0; i < 3; i++) {
            String nickname = i == 0 ? base : base + "_" + i;
            try {
                savedUser = userRepository.saveAndFlush(Users.builder()
                        .password(passwordEncoder.encode("social_" + externalId.hashCode()))
                        .nickName(nickname)
                        .gender(null).role(Role.USER).status(UserStatus.TEMP)
                        .build());
                log.info("신규 소셜 사용자 생성: id={}, nickname={}", savedUser.getId(), nickname);
                break;
            } catch (DataIntegrityViolationException e) {
                if (i == 2) throw new GeneralException(ErrorStatus._DUPLICATE_NICKNAME);
            }
        }
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
}

