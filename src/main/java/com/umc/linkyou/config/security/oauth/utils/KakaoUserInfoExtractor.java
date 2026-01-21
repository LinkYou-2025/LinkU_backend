package com.umc.linkyou.config.security.oauth.utils;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class KakaoUserInfoExtractor implements OAuth2UserInfoExtractor {

    @Override
    public String getExternalId(OAuth2User oAuth2User) {
        return Objects.requireNonNull(oAuth2User.getAttribute("id")).toString();
    }

    @Override
    public String getEmail(OAuth2User oAuth2User) {
        Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
        if (kakaoAccount != null) {
            return (String) kakaoAccount.get("email");
        }
        return null;
    }

    @Override
    public String getName(OAuth2User oAuth2User) {
        Map<String, Object> properties = oAuth2User.getAttribute("properties");
        if (properties != null) {
            return (String) properties.get("nickname");
        }
        return null;
    }
}
