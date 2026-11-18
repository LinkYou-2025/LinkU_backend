package com.umc.linkyou.domain.enums;

import java.util.Locale;

// Gemini가 판별한 페이지 종류
public enum PageType {
    CONTENT, // 글, 기사, 문서 등 실제 콘텐츠
    LOGIN, // 로그인, 회원가입 요구 화면
    ERROR, // 없는 페이지, 삭제된 글, 오류 안내
    PAYWALL, // 유료 구독, 성인 인증 등 접근 제한 안내
    OTHER, // 쿠키 동의, 중간 안내, 빈 페이지 등 콘텐츠가 아닌 화면
    UNKNOWN; // AI가 판별하지 못함 (값 누락, 알 수 없는 값)

    public static PageType from(String raw) {
        if (raw == null || raw.isBlank()) {
            return UNKNOWN;
        }
        try {
            return PageType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
