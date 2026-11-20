package com.umc.linkyou.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

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

            normalized = lowerCaseSchemeAndHost(normalized);

        } catch (MalformedURLException e) {
            return normalized;
        }

        return normalized;
    }

    // http, 호스트만 소문자로 변경
    private static String lowerCaseSchemeAndHost(String url) {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd < 0) {
            return url;
        }
        int authorityStart = schemeEnd + 3;
        int authorityEnd = url.length();
        for (int i = authorityStart; i < url.length(); i++) {
            char c = url.charAt(i);
            if (c == '/' || c == '?' || c == '#') {
                authorityEnd = i;
                break;
            }
        }

        String authority = url.substring(authorityStart, authorityEnd);
        // user:password@ 부분은 대소문자를 유지
        int at = authority.lastIndexOf('@');
        String userInfo = at < 0 ? "" : authority.substring(0, at + 1);
        String hostAndPort = authority.substring(at + 1).toLowerCase(Locale.ROOT);

        return url.substring(0, schemeEnd).toLowerCase(Locale.ROOT)
                + "://"
                + userInfo
                + hostAndPort
                + url.substring(authorityEnd);
    }
}
