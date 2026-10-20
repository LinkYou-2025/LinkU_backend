package com.umc.linkyou.oauth2.utils;

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
        if (authAccountOpt.isPresent()) {
            return reLogin(authAccountOpt.get(), email, name, profileImage, socialToken);
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

    // 재로그인: 소셜 토큰만 갱신, 닉네임/프로필은 더 이상 동기화 안 함
    private Users reLogin(AuthAccount authAccount, String email, String name,
                          String profileImage, String socialToken) {
        Users user = authAccount.getUser();
        if (socialToken != null) authAccount.updateToken(socialToken);
        return user;
    }

    /** 모바일용 (socialToken 없음) */
    public Users findOrCreateUser(String email, String name, String externalId,
                                  String profileImage, Provider provider) {
        return findOrCreateUser(email, name, externalId, profileImage, provider, null);
    }

    /** 3.완전 신규 사용자 + AuthAccount 일괄 생성*/
    private Users createNewUserWithAccount(String email, String name, Provider provider,
                                           String externalId, String profileImage, String socialToken) {

        String temporaryNickname = generateTemporaryNickname();

        Users savedUser = userRepository.saveAndFlush(Users.builder()
                .password(passwordEncoder.encode("social_" + externalId.hashCode()))
                .nickName(temporaryNickname)
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

    private String generateTemporaryNickname() {
        for (int attempt = 0; attempt < 3; attempt++) {
            String candidate = "LINKU_" + UUID.randomUUID().toString().replace("-", "");
            if (!userRepository.existsByNickName(candidate)) {
                return candidate;
            }
        }

        log.error("임시 닉네임 생성 실패");
        throw new GeneralException(ErrorStatus._INTERNAL_SERVER_ERROR);
    }
}
