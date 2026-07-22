package com.umc.linkyou.support.fixture;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Role;

public class CurationFixture {
    public static final Long USER_ID = 1L;
    public static final Long OTHER_USER_ID = 2L;
    public static final Long CURATION_ID = 1L;
    public static final String MONTH = "2026-05";

    public static Users user() {
        return Users.builder()
                .id(USER_ID)
                .role(Role.USER)
                .build();
    }

    public static Curation curation(Users user, String month) {
        return Curation.builder().user(user).baseMonth(month).build();
    }
}

