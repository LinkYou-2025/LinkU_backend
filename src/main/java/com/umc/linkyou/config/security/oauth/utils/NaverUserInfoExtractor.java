package com.umc.linkyou.config.security.oauth.utils;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class NaverUserInfoExtractor implements OAuth2UserInfoExtractor {

    @Override
    public String getExternalId(OAuth2User oAuth2User) {
        Map<String, Object> response = oAuth2User.getAttribute("response");
        if (response != null) {
            return Objects.requireNonNull(response.get("id")).toString();
        }
        return null;
    }

    @Override
    public String getEmail(OAuth2User oAuth2User) {
        Map<String, Object> response = oAuth2User.getAttribute("response");
        if (response != null) {
            return (String) response.get("email");
        }
        return null;
    }

    @Override
    public String getName(OAuth2User oAuth2User) {
        Map<String, Object> response = oAuth2User.getAttribute("response");
        if (response != null) {
            return (String) response.get("nickname");
        }
        return null;
    }
}
