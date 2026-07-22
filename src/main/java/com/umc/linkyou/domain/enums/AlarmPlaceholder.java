package com.umc.linkyou.domain.enums;

/**
 * 알림 본문 템플릿의 치환 placeholder 키를 한 곳에서 정의한다.
 * "{nickname}" 같은 매직 스트링이 여러 곳에 흩어지는 것을 막고,
 * 템플릿({token})과 값 Map 키(key)의 단일 출처 역할을 한다.
 */
public enum AlarmPlaceholder {

    NICKNAME("nickname"),
    FOLDER_NAME("folderName"),
    LINK_TITLE("linkTitle");

    private final String key;

    AlarmPlaceholder(String key) {
        this.key = key;
    }

    /** 값 Map 의 key. 예) "nickname" */
    public String key() {
        return key;
    }

    /** 템플릿에 들어가는 토큰. 예) "{nickname}" */
    public String token() {
        return "{" + key + "}";
    }
}