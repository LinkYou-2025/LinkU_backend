package com.umc.linkyou.utils;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlUtils {

    // 입력 URL을 정규화합니다.
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

            normalized = normalized.toLowerCase();

        } catch (MalformedURLException e) {
            return normalized;
        }

        return normalized;
    }
}
