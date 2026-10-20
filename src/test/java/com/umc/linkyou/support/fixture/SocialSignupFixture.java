package com.umc.linkyou.support.fixture;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;

public final class SocialSignupFixture {
    public static final String NICKNAME = "완성닉네임";
    public static final String TEMP_NICKNAME = "LINKU_0123456789abcdef0123456789abcdef";

    private SocialSignupFixture() {}

    public static Users tempUser(Long id) {
        return Users.builder().id(id).nickName(TEMP_NICKNAME).status(UserStatus.TEMP).build();
    }
}
