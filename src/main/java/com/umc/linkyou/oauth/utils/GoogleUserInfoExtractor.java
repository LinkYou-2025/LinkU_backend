
package com.umc.linkyou.oauth.utils;

import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class GoogleUserInfoExtractor implements OAuth2UserInfoExtractor {

    @Override
    public String getExternalId(OAuth2User oAuth2User) {
        return oAuth2User.getAttribute("sub");
    }

    @Override
    public String getEmail(OAuth2User oAuth2User) {
        return oAuth2User.getAttribute("email");
    }

    @Override
    public String getName(OAuth2User oAuth2User) {
        return oAuth2User.getAttribute("name");
    }
}
