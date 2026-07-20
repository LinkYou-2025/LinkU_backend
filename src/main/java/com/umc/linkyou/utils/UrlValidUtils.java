package com.umc.linkyou.utils;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

public class UrlValidUtils {
    /*
    * 링크 생성/ 수정 시 정상 url확인
    * */
    public static void validateLinkuUrl(String url) {
        if (isVideoLink(url)) {
            throw new GeneralException(LinkuErrorStatus._LINKU_VIDEO_NOT_ALLOWED);
        }

        if (!isValidUrl(url)) {
            throw new GeneralException(LinkuErrorStatus._LINKU_INVALID_URL);
        }
    }

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
     * URL 문법 형식만 체크 (접속 시도하지 않음)
     */
    public static boolean isValidUrl(String url) {
        try {
            new URL(url); // 문법적으로 유효한지 검사
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    // 실제 HTTP 연결이 정상인지 확인하는 로직은 SSRF 검증이 필요해서
    // com.umc.linkyou.infra.net.SafeUrlFetcher#isReachable(String)로 옮겼다.

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
