package com.umc.linkyou.config.security.oauth;

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
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("email_required", "이메일이 필요합니다.", null)
                );
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
            Optional<Users> userOpt = usersRepository.findByEmail(email);

            if (userOpt.isPresent()) {
                user = userOpt.get();
                isNewUser = false;
            } else {
                user = createNewUser(email, name);
                isNewUser = true;
            }

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
            Users user = Users.builder()
                    .email(email)
                    .password(null)
                    .nickName(name != null ? name : "사용자")
                    .gender(null)
                    .role(Role.USER)
                    .status("ACTIVE")
                    .build();
            user = usersRepository.save(user);
            entityManager.flush();
            return user;
        } catch (Exception e) {
            log.error("Users 저장 실패: email={}", email, e);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("user_creation_failed", "사용자 생성 중 오류가 발생했습니다: " + e.getMessage(), null),
                    e
            );
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
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("auth_account_creation_failed", "인증 계정 생성 중 오류가 발생했습니다: " + e.getMessage(), null),
                    e
            );
        }
    }

    private Provider getProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> Provider.GOOGLE;
            case "kakao"  -> Provider.KAKAO;
            case "naver"  -> Provider.NAVER;
            default -> throw new IllegalArgumentException("Unsupported provider: " + registrationId);
        };
    }

    private OAuth2UserInfoExtractor getExtractor(Provider provider) {
        return switch (provider) {
            case GOOGLE -> googleUserInfoExtractor;
            case KAKAO  -> kakaoUserInfoExtractor;
            case NAVER  -> naverUserInfoExtractor;
            //각 enum별로 작성하지 않으면 에러남
            default -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }


    private String generateTemporaryEmail(Provider provider, String externalId) {
        return String.format("%s_%s@%s.linkyou",
                provider.name().toLowerCase(),
                externalId,
                provider.name().toLowerCase());
    }
}
