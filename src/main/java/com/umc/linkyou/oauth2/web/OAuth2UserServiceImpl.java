package com.umc.linkyou.oauth2.web;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.enums.UserStatus;
import com.umc.linkyou.oauth2.UserSocialLoginHelper;
import com.umc.linkyou.oauth2.utils.CustomOAuth2User;
import com.umc.linkyou.oauth2.utils.GoogleUserInfoExtractor;
import com.umc.linkyou.oauth2.utils.KakaoUserInfoExtractor;
import com.umc.linkyou.oauth2.utils.NaverUserInfoExtractor;
import com.umc.linkyou.oauth2.utils.OAuth2UserInfoExtractor;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
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

    private final UserSocialLoginHelper userSocialLoginHelper;
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
        String socialToken = userRequest.getAccessToken().getTokenValue();
        Users user = userSocialLoginHelper.findOrCreateUser(
                email, name, externalId, profileImage, provider, socialToken);
        boolean isNewUser = user.getStatus() == UserStatus.TEMP;

        //OAuth 객체 반환
        return createCustomOAuth2User(user, oAuth2User, isNewUser, externalId, provider);
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
