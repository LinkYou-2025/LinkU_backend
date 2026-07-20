package com.umc.linkyou.utils;

import com.google.common.net.InternetDomainName;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
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

    /**
     * URL 호스트로부터 domains 테이블 조회에 쓸 후보 tail 목록을 우선순위 순으로 반환한다.
     *
     * 1순위: 호스트 그대로(www. 제거). blog.naver.com처럼 특정 서브도메인 전용 전략이
     *        seed되어 있는 경우 그대로 적중시키기 위함.
     * 2순위: registry suffix(ICANN이 관리하는 TLD, 예: com/co.kr/io) 바로 아래의 도메인.
     *        someuser.tistory.com → tistory.com, someuser.blogspot.com → blogspot.com,
     *        someuser.github.io → github.io 처럼 {user}.platform.tld 구조의 서비스가
     *        플랫폼 apex 행에 자동으로 매칭되도록 하기 위함.
     *
     * 2순위는 Guava InternetDomainName#topDomainUnderRegistrySuffix()를 사용한다.
     * 이는 Public Suffix List(PSL) 중 ICANN(레지스트리) 구간만 기준으로 삼는 메서드로,
     * PSL의 PRIVATE 구간(tistory.com/blogspot.com/github.io 등이 등재되어 있을 수 있음)을
     * 함께 쓰는 topPrivateDomain()과 달리 PRIVATE 등재 여부와 무관하게 항상
     * "레지스트리 TLD 바로 아래 라벨"을 반환한다. topPrivateDomain()을 썼다면 tistory.com이
     * PRIVATE 섹션에 등재되어 있을 경우 someuser.tistory.com이 그 자체로 최상위 사설 도메인으로
     * 취급되어 tistory.com으로 축약되지 않는 문제가 생길 수 있는데, 이 방식은 그 문제를 피한다.
     */
    public static List<String> extractDomainTailCandidates(String url) {
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return List.of();
            }
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            candidates.add(host);

            if (InternetDomainName.isValid(host)) {
                InternetDomainName idn = InternetDomainName.from(host);
                // hasRegistrySuffix()가 false면(예: localhost, 사설 IP 형태 호스트) apex를 계산할 수 없고,
                // isRegistrySuffix()가 true면(호스트 자체가 "com" 같은 registry suffix) 그 아래 라벨이 없다.
                if (idn.hasRegistrySuffix() && !idn.isRegistrySuffix()) {
                    candidates.add(idn.topDomainUnderRegistrySuffix().toString());
                }
            }
        } catch (Exception e) {
            // 파싱 실패 시 지금까지 모은 후보만 반환 (보통 비어있거나 host만 담긴 상태).
            // 실제 파싱 버그와 "정상적으로 후보가 하나뿐인 경우"를 운영 중에 구분할 수 있도록 로그만 남긴다.
            log.debug("[도메인 tail 후보 계산 실패] url: {}, 이유: {}", url, e.getMessage());
        }
        return List.copyOf(candidates);
    }
}
