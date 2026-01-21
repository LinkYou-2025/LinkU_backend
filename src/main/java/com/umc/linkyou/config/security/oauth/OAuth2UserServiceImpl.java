package com.umc.linkyou.config.security.oauth;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.security.oauth.utils.CustomOAuth2User;
import com.umc.linkyou.config.security.oauth.utils.GoogleUserInfoExtractor;
import com.umc.linkyou.config.security.oauth.utils.KakaoUserInfoExtractor;
import com.umc.linkyou.config.security.oauth.utils.NaverUserInfoExtractor;
import com.umc.linkyou.config.security.oauth.utils.OAuth2UserInfoExtractor;
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
import org.springframework.security.oauth2.core.OAuth2Error;
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

        boolean needsEmailUpdate = false;
        if (email == null || email.isBlank()) {
            if (provider == Provider.KAKAO) {
                email = generateTemporaryEmail(provider, externalId);
                needsEmailUpdate = true;
            } else {

                throw new GeneralException(ErrorStatus._SOCIAL_EMAIL_REQUIRED);
            }
        }

        Users user;
        AuthAccount authAccount;
        boolean isNewUser = false;

        Optional<AuthAccount> authAccountOpt =
                authAccountRepository.findByProviderAndExternalId(provider, externalId);

        if (authAccountOpt.isPresent()) {
            authAccount = authAccountOpt.get();
            user = authAccount.getUser();

            if (name != null && !name.equals(user.getNickName())) {
                user.setNickName(name);
            }

            authAccount.updateToken(userRequest.getAccessToken().getTokenValue());
        } else {
            user = createNewUser(email, name);
            isNewUser = true;
            createAuthAccount(user, provider, externalId, userRequest);

        }

        boolean needsTermsAgreement = isNewUser;

        Map<String, Object> attributesWithFlag = new HashMap<>(oAuth2User.getAttributes());
        if (needsEmailUpdate) {
            attributesWithFlag.put("needsEmailUpdate", true);
            attributesWithFlag.put("temporaryEmail", email);
        }
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

            // 최대 3회 시도
            for (int i = 0; i < 3; i++) {
                String finalNickname = i == 0 ? nickname : nickname + "_" + i;

                Users user = Users.builder()
                        .email(email)
                        .password(null)
                        .nickName(finalNickname)
                        .gender(null)
                        .role(Role.USER)
                        .status("ACTIVE")
                        .build();

                Users savedUser = usersRepository.saveAndFlush(user);
                log.info("소셜 사용자 생성: id={}, nickName={}, from={}",
                        savedUser.getId(), finalNickname, name != null ? "name" : "email");
                entityManager.flush();
                return savedUser;
            }

            log.error("닉네임 생성 완전 실패: email={}, 모든 후보 중복", email);
            throw new GeneralException(ErrorStatus._DUPLICATE_NICKNAME);
        } catch (DataIntegrityViolationException e) {
            log.warn("닉네임 중복: email={}, error={}", email, e.getMessage());
            throw new GeneralException(ErrorStatus._DUPLICATE_NICKNAME);
        } catch (Exception e) {
            log.error("사용자 생성 실패: email={}", email, e);
            throw new GeneralException(ErrorStatus._USER_SOCIAL_CREATION_FAILED);
        }
    }

    private AuthAccount createAuthAccount(Users user, Provider provider, String externalId, OAuth2UserRequest userRequest) {
        try {
            return authAccountRepository.save(
                    AuthAccount.builder()
                            .user(user)
                            .provider(provider)
                            .externalId(externalId)
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


    private String generateTemporaryEmail(Provider provider, String externalId) {
        return String.format("%s_%s@%s.linkyou",
                provider.name().toLowerCase(),
                externalId,
                provider.name().toLowerCase());
    }
}
