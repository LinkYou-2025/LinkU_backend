package com.umc.linkyou.utils;

import javax.net.ssl.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.cert.X509Certificate;
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
        System.out.println("[CHECK] 입력 URL: " + url);

        // 1. URL 문법 체크
        try {
            URL parsed = new URL(url);
            System.out.println("[INFO] URL 파싱 성공: " + parsed);
        } catch (MalformedURLException e) {
            System.out.println("[ERROR] URL 형식 에러: " + e.getMessage());
            return false;
        }

        // 2. SSL 검증 우회 위한 TrustManager, HostnameVerifier 설정
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

        } catch (Exception e) {
            System.out.println("[ERROR] SSL 컨텍스트 초기화 실패: " + e.getMessage());
            // SSL 우회가 안되면 그냥 진행(try문 밖으로)
        }

        // 3. HTTP GET 요청으로 확인
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36");

            System.out.println("[INFO] 연결 시도 → " + connection.getURL());

            int responseCode = connection.getResponseCode();
            System.out.println("[INFO] 응답 코드: " + responseCode);

            boolean result = responseCode >= 200 && responseCode < 400;
            System.out.println("[RESULT] 유효 여부: " + result);
            return result;

        } catch (Exception e) {
            System.out.println("[ERROR] 연결 실패: " + e.getClass().getSimpleName() + " → " + e.getMessage());
            return false;
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
