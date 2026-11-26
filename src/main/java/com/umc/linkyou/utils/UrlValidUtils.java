package com.umc.linkyou.utils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

import com.google.common.net.InternetDomainName;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UrlValidUtils {
    public static void validateLinkuUrl(String url) {
        if (isVideoLink(url)) {
            throw new GeneralException(LinkuErrorStatus._LINKU_VIDEO_NOT_ALLOWED);
        }

        if (!isValidUrl(url)) {
            throw new GeneralException(LinkuErrorStatus._LINKU_INVALID_URL);
        }
    }

    // DB에는 정규화된 URL이 저장되므로 조회, 저장에는 반환값을 쓴다.
    public static String normalizeAndValidateLinkuUrl(String url) {
        String normalized = UrlUtils.normalizeUrl(url);
        validateLinkuUrl(normalized);
        return normalized;
    }

    public static boolean isVideoLink(String url) {
        List<String> videoDomains =
                List.of(
                        "youtube.com",
                        "youtu.be",
                        "vimeo.com",
                        "tiktok.com",
                        "dailymotion.com",
                        "kakao.tv",
                        "navertv",
                        "tv.naver.com",
                        "tv.kakao.com");

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null) return false;

            return videoDomains.stream().anyMatch(host::contains);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    // 문법만 확인
    public static boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static String extractDomainTail(String url) {
        try {
            URI uri = new URI(url);
            return normalizeDomainTail(uri.getHost());
        } catch (Exception e) {
            return null;
        }
    }

    public static String normalizeDomainTail(String domainTail) {
        if (domainTail == null || domainTail.isBlank()) {
            return null;
        }
        String normalized = domainTail.trim().toLowerCase(Locale.ROOT);
        while (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized.startsWith("www.") ? normalized.substring(4) : normalized;
    }

    // 호스트 그대로 넣어 서브도메인 전용 행을 우선 매칭하고,
    // 등록 도메인을 넣어 사용자별 서브도메인을 플랫폼 행에 매칭한다.
    // topPrivateDomain()은 PSL 사설 구간에 등재된 플랫폼 주소를 줄이지 못해 쓰지 않는다.
    public static List<String> extractDomainTailCandidates(String url) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        try {
            URI uri = new URI(url);
            String host = normalizeDomainTail(uri.getHost());
            if (host == null || host.isBlank()) {
                return List.of();
            }
            candidates.add(host);

            if (InternetDomainName.isValid(host)) {
                InternetDomainName idn = InternetDomainName.from(host);
                // localhost, IP나 TLD 자체인 호스트는 등록 도메인이 없다.
                if (idn.hasRegistrySuffix() && !idn.isRegistrySuffix()) {
                    candidates.add(idn.topDomainUnderRegistrySuffix().toString());
                }
            }
        } catch (Exception e) {
            // 실패하면 기본 도메인에 연결되므로 원인 URL을 남긴다.
            log.debug("[도메인 tail 후보 계산 실패] url: {}, 이유: {}", url, e.getMessage());
        }
        return List.copyOf(candidates);
    }
}
