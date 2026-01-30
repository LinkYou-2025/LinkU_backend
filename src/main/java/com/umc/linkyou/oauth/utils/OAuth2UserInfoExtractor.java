package com.umc.linkyou.oauth.utils;

import org.springframework.security.oauth2.core.user.OAuth2User;

public interface OAuth2UserInfoExtractor {

    String getExternalId(OAuth2User oAuth2User);

    String getEmail(OAuth2User oAuth2User);

    String getName(OAuth2User oAuth2User);
}