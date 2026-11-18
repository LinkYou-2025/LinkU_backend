package com.umc.linkyou.service.Linku;

import java.net.URI;

public final class LinkuTitleResolver {

    private static final int MAX_FALLBACK_TITLE_LENGTH = 50;

    private LinkuTitleResolver() {}

    public static String resolve(String aiTitle, String domainTail, String url) {
        if (aiTitle != null && !aiTitle.isBlank()) {
            return aiTitle;
        }
        String fallback = buildFallbackTitle(domainTail, url);
        return fallback != null && !fallback.isBlank() ? fallback : "제목 없음";
    }

    // AI 제목이 없으면 도메인과 경로로 제목을 만든다.
    private static String buildFallbackTitle(String domainTail, String url) {
        if (domainTail == null || domainTail.isBlank()) return null;
        String path = null;
        try {
            path = new URI(url).getPath();
        } catch (Exception e) {
            // 파싱에 실패하면 도메인만 쓴다.
        }
        String combined =
                path != null && !path.isBlank() && !path.equals("/")
                        ? domainTail + path
                        : domainTail;
        return combined.length() > MAX_FALLBACK_TITLE_LENGTH
                ? combined.substring(0, MAX_FALLBACK_TITLE_LENGTH) + "…"
                : combined;
    }
}
