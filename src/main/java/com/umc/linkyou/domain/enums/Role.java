package com.umc.linkyou.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum Role {
    //spring security의 hasRole은 내부적으로 "ROLE_" 접두사를 체크한다.
    USER("ROLE_USER"), //일반 사용자
    ADMIN("ROLE_ADMIN"), // 개발자
    MANAGER("ROLE_MANAGER"); //운영자, 콘텐츠 관리자

    private final String authority;

    public static Role fromAuthority(String authority) {
        return Arrays.stream(values())
                .filter(r -> r.authority.equals(authority))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown authority: " + authority));
    }
}
