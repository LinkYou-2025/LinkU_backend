package com.umc.linkyou.utils;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class UrlValidUtils {
    /**
     * 영상 플랫폼 링크 여부 판별
     */
    public static boolean isVideoLink(String url) {
        // 영상 플랫폼 도메인 리스트
        List<String> videoDomains = List.of(
                "youtube.com", "youtu.be", "vimeo.com", "tiktok.com",
                "dailymotion.com", "kakao.tv", "navertv", "tv.kakao.com"
        );

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return false;

            return videoDomains.stream().anyMatch(host::contains);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * URL 형식 & 실제 접속 가능 여부 체크
     */
    public static boolean isValidUrl(String url) {
        // 1. URL 문법 체크
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            return false;
        }

        // 2. HTTP GET 요청으로 확인
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true);

            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        } catch (Exception e) {
            return false;
        }
    }
}
