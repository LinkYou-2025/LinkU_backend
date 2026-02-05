package com.umc.linkyou.oauth;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth.utils.CustomOAuth2User;
import com.umc.linkyou.oauth.utils.GoogleUserInfoExtractor;
import com.umc.linkyou.oauth.utils.KakaoUserInfoExtractor;
import com.umc.linkyou.oauth.utils.NaverUserInfoExtractor;
import com.umc.linkyou.oauth.utils.OAuth2UserInfoExtractor;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.repository.authAccountRepository.AuthAccountRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OAuth2UserServiceImpl implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository usersRepository;
    private final AuthAccountRepository authAccountRepository;

    private final GoogleUserInfoExtractor googleUserInfoExtractor;
    private final KakaoUserInfoExtractor kakaoUserInfoExtractor;
    private final NaverUserInfoExtractor naverUserInfoExtractor;

    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = getProvider(registrationId);

        OAuth2User oAuth2User = delegate.loadUser(userRequest);
        OAuth2UserInfoExtractor extractor = getExtractor(provider);

        String externalId = extractor.getExternalId(oAuth2User);
        String email = extractor.getEmail(oAuth2User);
        String name = extractor.getName(oAuth2User);
        String profileImage = extractor.getProfileImage(oAuth2User);

        if (email == null || email.isBlank()) {
            throw new GeneralException(ErrorStatus._SOCIAL_EMAIL_REQUIRED);  // 모든 소셜에서 이메일 필수!
        }

        Users user;
        AuthAccount authAccount;
        boolean isNewUser = false;

        Optional<AuthAccount> authAccountOpt =
                authAccountRepository.findByProviderAndExternalId(provider, externalId);

        if (authAccountOpt.isPresent()) {
            authAccount = authAccountOpt.get();
            user = authAccount.getUser();

            // 기존 사용자 닉네임 업데이트 (중복 체크)
            if (name != null && !name.equals(user.getNickName())) {
                if (!usersRepository.existsByNickName(name)) {
                    user.setNickName(name);
                    log.info("기존 사용자 닉네임 업데이트: id={}, old={}, new={}",
                            user.getId(), user.getNickName(), name);
                } else {
                    log.warn("닉네임 업데이트 스킵 (중복): userId={}, requested={}",
                            user.getId(), name);
                }
            }
            if (profileImage != null && !profileImage.equals(authAccount.getProfileImage())) {
                authAccount.updateProfileImage(profileImage);
                log.info("소셜 프로필 이미지 업데이트: provider={}, userId={}, image={}",
                        provider, user.getId(), profileImage);
            }

            authAccount.updateToken(userRequest.getAccessToken().getTokenValue());
        } else {
            // 1. 이메일로 기존 사용자 조회
            Optional<Users> existingUserOpt = usersRepository.findByEmail(email);

            if (existingUserOpt.isPresent()) {
                // 기존 사용자에 AuthAccount만 연결 (새 사용자 아님)
                user = existingUserOpt.get();
                log.info("기존 사용자에 소셜 계정 연결: userId={}, provider={}, email={}",
                        user.getId(), provider, email);
                createAuthAccount(user, provider, externalId, userRequest, profileImage);
                isNewUser = false;
            } else {
                // 진짜 새 사용자 생성
                user = createNewUser(email, name);
                isNewUser = true;
                createAuthAccount(user, provider, externalId, userRequest, profileImage);
            }
        }
        boolean needsTermsAgreement = isNewUser;

        Map<String, Object> attributesWithFlag = new HashMap<>(oAuth2User.getAttributes());
        if (needsTermsAgreement) {
            attributesWithFlag.put("needsTermsAgreement", true);
        }

        return new CustomOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())),
                attributesWithFlag,
                "email",
                user.getEmail()
        );
    }

    private Users createNewUser(String email, String name) {
        try {
            String nickname;
            if (name != null && !name.isBlank()) {
                nickname = name;
            } else {
                // email@domain.com → domain
                String domain = email.substring(email.indexOf("@") + 1);
                nickname = domain.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (nickname.isBlank()) nickname = "user";
            }

            // 최대 3회 시도 (중복시 다음으로 계속)
            for (int i = 0; i < 3; i++) {
                String finalNickname = i == 0 ? nickname : nickname + "_" + i;

                Users user = Users.builder()
                        .email(email)
                        .password(null)
                        .nickName(finalNickname)
                        .gender(null)
                        .role(Role.USER)
                        .status(UserStatus.TEMP)
                        .build();

                try {
                    Users savedUser = usersRepository.saveAndFlush(user);
                    log.info("소셜 사용자 생성: id={}, nickName={}, from={}",
                            savedUser.getId(), finalNickname, name != null ? "name" : "email");
                    entityManager.flush();
                    return savedUser;
                } catch (DataIntegrityViolationException e) {
                    log.warn("닉네임 중복 (시도 {}/3): {}", i+1, finalNickname);
                    if (i == 2) {
                        log.error("닉네임 생성 완전 실패: email={}, 모든 후보 중복", email);
                        throw new GeneralException(ErrorStatus._DUPLICATE_NICKNAME);
                    }
                }
            }
            throw new GeneralException(ErrorStatus._DUPLICATE_NICKNAME);
        } catch (Exception e) {
            log.error("사용자 생성 실패: email={}", email, e);
            throw new GeneralException(ErrorStatus._USER_SOCIAL_CREATION_FAILED);
        }
    }

    private AuthAccount createAuthAccount(Users user, Provider provider, String externalId, OAuth2UserRequest userRequest, String profileImage) {
        try {
            return authAccountRepository.save(
                    AuthAccount.builder()
                            .user(user)
                            .provider(provider)
                            .externalId(externalId)
                            .profileImage(profileImage)
                            .socialToken(userRequest.getAccessToken().getTokenValue())
                            .build()
            );
        } catch (Exception e) {
            log.error("AuthAccount 저장 실패: user.id={}, provider={}", user.getId(), provider, e);
            throw new GeneralException(ErrorStatus._AUTH_ACCOUNT_SAVE_FAILED);
        }
    }

    private Provider getProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> Provider.GOOGLE;
            case "kakao"  -> Provider.KAKAO;
            case "naver"  -> Provider.NAVER;

            default -> throw new GeneralException(ErrorStatus._SOCIAL_UNSUPPORTED_PROVIDER);
        };
    }

    private OAuth2UserInfoExtractor getExtractor(Provider provider) {
        return switch (provider) {
            case GOOGLE -> googleUserInfoExtractor;
            case KAKAO  -> kakaoUserInfoExtractor;
            case NAVER  -> naverUserInfoExtractor;
            default -> throw new GeneralException(ErrorStatus._SOCIAL_UNSUPPORTED_PROVIDER);
        };
    }

}
