package com.umc.linkyou.support.fixture;

import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.enums.Provider;

public class AuthAccountFixture {

    static final String EMAIL = "user@example.com";

    static AuthAccount generalAuthAccount() {
        return AuthAccount.builder()
                .email(EMAIL)
                .provider(Provider.GENERAL)
                .build();
    }

    static AuthAccount kakaoAuthAccount() {
        return AuthAccount.builder()
                .email(EMAIL)
                .provider(Provider.KAKAO)
                .build();
    }

    static AuthAccount googleAuthAccount() {
        return AuthAccount.builder()
                .email(EMAIL)
                .provider(Provider.GOOGLE)
                .build();
    }
}