package com.umc.linkyou.oauth.web;

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

    //소셜 api에서 받은 OAuth 객체
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 어느 소셜에서 보내는 지 확인
        // Spring Security에서 파싱한다.
        // 프론트가 OAuthConroller로 보낸다 -> 리다이렉트: /oauth2/authorization/{registrationId} → registrationId="kakao"
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Provider provider = getProvider(registrationId);

        // 소셜 API 요청 및 JSON Parsing
        OAuth2User oAuth2User = delegate.loadUser(userRequest); //리다이렉트 요청이 들어오면 그걸 받아서 소셜 API호출
        OAuth2UserInfoExtractor extractor = getExtractor(provider); //어떤 소셜인지에 따라 json파서 선택

        //parsing된 값들
        String externalId = extractor.getExternalId(oAuth2User);
        String email = extractor.getEmail(oAuth2User);
        String name = extractor.getName(oAuth2User);
        String profileImage = extractor.getProfileImage(oAuth2User);

        // 이메일 검증
        if (email == null || email.isBlank()) {
            throw new GeneralException(ErrorStatus._SOCIAL_EMAIL_REQUIRED);  // 모든 소셜에서 이메일 필수!
        }
        // 3가지 케이스에 따라 다르게 DB 접근 및 저장
        // 케이스1) 소셜로그인한적이 있다. 새로 User, AuthAccount객체를 생성하지 않는다.
        // 케이스2) JWT일반로그인이나 다른 소셜로그인을 한적 있지만 해당 소셜로그인을 한적 없다. 기존 User에 AuthAccount 객체를 생성하여 연결한다.
        // 케이스3) JWT일반 로그인도 소셜로그인도 한 적이 없다. User, AuthAccount객체 모두 생성한다.
        Users user = findOrCreateUser(email, name, provider, externalId, userRequest, profileImage);
        boolean isNewUser = user.getStatus() == UserStatus.TEMP;

        //OAuth 객체 반환
        return createCustomOAuth2User(user, oAuth2User, isNewUser, externalId, provider);
    }


    /**
     * 모든 사용자 처리 케이스를 한 메서드로!
     */
    private Users findOrCreateUser(String email, String name, Provider provider,
                                   String externalId, OAuth2UserRequest userRequest, String profileImage) {
        // 케이스 1: 이미 이 소셜 계정으로 로그인한 적 있음 (재로그인)
        Optional<AuthAccount> authAccountOpt = authAccountRepository.findByProviderAndExternalId(provider, externalId);
        if (authAccountOpt.isPresent()) {
            return updateExistingUser(authAccountOpt.get(), name, profileImage, userRequest);
        }

        // 케이스 2: 이메일은 있지만 다른 소셜 계정 (소셜 연결)
        Optional<Users> existingUserOpt = usersRepository.findByEmail(email);
        if (existingUserOpt.isPresent()) {
            Users user = existingUserOpt.get();
            log.info("기존 사용자에 소셜 계정 연결: userId={}, provider={}, email={}",
                    user.getId(), provider, email);
            createAuthAccount(user, provider, externalId, userRequest, profileImage);
            return user;
        }

        // 케이스 3: 완전 새로운 사용자
        log.info("새 소셜 사용자 생성: provider={}, email={}", provider, email);
        return createNewUserWithAccount(email, name, provider, externalId, userRequest, profileImage);
    }
    /** 1. 재로그인: 닉네임/프로필/토큰 업데이트 */
    private Users updateExistingUser(AuthAccount authAccount, String name, String profileImage, OAuth2UserRequest userRequest) {
        Users user = authAccount.getUser();

        // 닉네임 업데이트 (Users 공통, 중복 체크)
        if (name != null && !name.equals(user.getNickName()) && !usersRepository.existsByNickName(name)) {
            user.setNickName(name);
            log.info("기존 사용자 닉네임 업데이트: id={}, old={}, new={}",
                    user.getId(), user.getNickName(), name);
        }

        // 프로필 이미지 업데이트 (AuthAccount 소셜별)
        if (profileImage != null && !profileImage.equals(authAccount.getProfileImage())) {
            authAccount.updateProfileImage(profileImage);
            log.info("소셜 프로필 이미지 업데이트: provider={}, userId={}",
                    authAccount.getProvider(), user.getId(), profileImage);
        }

        authAccount.updateToken(userRequest.getAccessToken().getTokenValue());
        return user;
    }
    /** 2. 이메일이 있지만 다른 소셜로그인 계정 + 다른 곳에서도 쓰는 메서드*/
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
                    log.warn("닉네임 중복 (시도 {}/3): {}", i + 1, finalNickname);
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

    /** 3.새 사용자 + AuthAccount 일괄 생성*/
    private Users createNewUserWithAccount(String email, String name, Provider provider,
                                           String externalId, OAuth2UserRequest userRequest, String profileImage) {
        Users user = createNewUser(email, name);  // 기존 메서드 재사용
        createAuthAccount(user, provider, externalId, userRequest, profileImage);
        return user;
    }

    /**Spring Security에 저잫할 Oauth 객체 생성*/
    private CustomOAuth2User createCustomOAuth2User(Users user, OAuth2User oAuth2User, boolean isNewUser
            ,String externalId, Provider provider) {
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        if (isNewUser) {
            attributes.put("needsTermsAgreement", true);
        }
        return new CustomOAuth2User(
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole().name())),
                attributes, "email", user.getEmail(), externalId, provider.name()
        );
    }



    private Provider getProvider(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> Provider.GOOGLE;
            case "kakao" -> Provider.KAKAO;
            case "naver" -> Provider.NAVER;

            default -> throw new GeneralException(ErrorStatus._SOCIAL_UNSUPPORTED_PROVIDER);
        };
    }

    private OAuth2UserInfoExtractor getExtractor(Provider provider) {
        return switch (provider) {
            case GOOGLE -> googleUserInfoExtractor;
            case KAKAO -> kakaoUserInfoExtractor;
            case NAVER -> naverUserInfoExtractor;
            default -> throw new GeneralException(ErrorStatus._SOCIAL_UNSUPPORTED_PROVIDER);
        };
    }
}
