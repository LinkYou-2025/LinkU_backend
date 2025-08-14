package com.umc.linkyou.utils;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;

import javax.net.ssl.SSLHandshakeException;
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
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36");

            int responseCode = connection.getResponseCode();
            return responseCode >= 200 && responseCode < 400;
        } catch (SSLHandshakeException e) { //SSL 인증성 없음
            return false; // 컨트롤러에서 SUS_URL 처리
        } catch (Exception e) {
            throw new GeneralException(ErrorStatus._LINKU_INVALID_URL);
        }
    }


    /**
     * URL에서 도메인명만 추출 (예: https://blog.naver.com/abc → blog.naver.com)
     */
    public static String extractDomainTail(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain != null && domain.startsWith("www.")) {
                domain = domain.substring(4);
            }
            return domain;
        } catch (Exception e) {
            return null;
        }
    }
}
