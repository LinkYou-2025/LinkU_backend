package com.umc.linkyou.utils;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlUtils {

    /**
     * 입력 URL을 정규화합니다.
     * - 끝에 불필시 '/' 제거 (단, 루트 도메인은 보존)
     * - URL 파싱 실패 시 원본 반환
     * - 필요 시 소문자 변환, 쿼리 스트링 정리 등 추가 가능
     */
    public static String normalizeUrl(String url) {
        if (url == null) return null;

        String normalized = url.trim();

        try {
            URL parsedUrl = new URL(normalized);
            String path = parsedUrl.getPath();

            // path가 "/" 아닌데 끝이 '/'면 제거
            if (path != null && !path.equals("/") && normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }

            // 필요 시 소문자 변환 등 추가 가능
            // normalized = normalized.toLowerCase();

        } catch (MalformedURLException e) {
            // 파싱 실패 시 원본 반환
            return normalized;
        }

        return normalized;
    }
}
