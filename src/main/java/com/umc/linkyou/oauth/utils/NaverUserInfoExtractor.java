package com.umc.linkyou.oauth.utils;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NaverUserInfoExtractor implements OAuth2UserInfoExtractor {

    @Override
    public String getExternalId(OAuth2User oAuth2User) {
        Map<String, Object> response = oAuth2User.getAttribute("response");
        if (response != null) {
            Object id = response.get("id");
            if (id == null) {
                throw new IllegalStateException("Naver 응답에 id가 없습니다");
            }
            return id.toString();
        }
        throw new IllegalStateException("Naver 응답 데이터가 없습니다");
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

    @Override
    public String getProfileImage(OAuth2User oAuth2User) {
        Map<String, Object> response = oAuth2User.getAttribute("response");
        if (response != null) {
            return (String) response.get("profile_image");  // http://static.naver.net/...
        }
        return null;
    }
}
