package com.umc.linkyou.utils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.google.common.net.InternetDomainName;
import com.umc.linkyou.infra.net.SafeUrlFetcher.RedirectHop;

// 링크의 대표 URL(linkus.linku_url)을 정하는 규칙
public final class CanonicalUrlPolicy {

    private CanonicalUrlPolicy() {}

    private static final Set<Integer> PERMANENT_REDIRECT_CODES = Set.of(301, 308);
    private static final Set<String> DEVICE_HOST_PREFIXES = Set.of("www.", "m.", "mobile.");

    public static String followTrustedRedirects(String inputUrl, List<RedirectHop> hops) {
        String candidate = inputUrl;
        for (RedirectHop hop : hops) {
            if (!isTrusted(hop)) {
                break;
            }
            candidate = hop.toUrl();
        }
        // 경로가 있던 URL이 홈으로 가면 글이 없는 경우라 따라가지 않는다.
        if (!candidate.equals(inputUrl) && collapsedToRoot(inputUrl, candidate)) {
            return finish(inputUrl);
        }
        return finish(candidate);
    }

    // 영구 이동과 다른 사이트로 가는 임시 이동만 따라간다.
    // 같은 사이트 안의 임시 이동은 로그인, 기기, 지역 분기라 멈춘다.
    static boolean isTrusted(RedirectHop hop) {
        if (PERMANENT_REDIRECT_CODES.contains(hop.status())) {
            return true;
        }
        return !sameSite(hop.fromUrl(), hop.toUrl());
    }

    // og:url은 모바일 주소일 수 있어 canonical을 먼저 본다.
    // 검증은 리다이렉트 후 도착한 URL(landingUrl)으로 한다.
    public static Optional<String> declaredUrl(Document doc, String landingUrl) {
        if (doc == null) {
            return Optional.empty();
        }
        return firstValid(
                landingUrl,
                attr(doc.selectFirst("link[rel~=(?i)^canonical$]"), "href"),
                attr(doc.selectFirst("meta[property=og:url]"), "content"));
    }

    public static boolean hasPasswordLoginForm(Document doc) {
        if (doc == null) {
            return false;
        }
        for (Element password :
                doc.select(
                        "form input[type=password], form input[autocomplete=current-password], form input[autocomplete=new-password]")) {
            // 숨겨 둔 로그인 템플릿은 무시한다.
            if (isHidden(password)) {
                continue;
            }
            String autocomplete = lower(password.attr("autocomplete"));
            if (autocomplete.equals("current-password") || autocomplete.equals("new-password")) {
                return true;
            }
            Element form = password.closest("form");
            if (form != null
                    && form.select("input[autocomplete=username], input[type=email]").stream()
                            .anyMatch(input -> !isHidden(input))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHidden(Element element) {
        for (Element current = element; current != null; current = current.parent()) {
            String style = lower(current.attr("style")).replace(" ", "");
            if (current.hasAttr("hidden")
                    || "true".equalsIgnoreCase(current.attr("aria-hidden"))
                    || style.contains("display:none")
                    || style.contains("visibility:hidden")) {
                return true;
            }
        }
        return false;
    }

    private static Optional<String> firstValid(String landingUrl, String... candidates) {
        for (String raw : candidates) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            Optional<String> resolved = resolveDeclared(raw.trim(), landingUrl);
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return Optional.empty();
    }

    private static Optional<String> resolveDeclared(String raw, String landingUrl) {
        URL landing = parse(landingUrl);
        if (landing == null) {
            return Optional.empty();
        }
        URL declared;
        try {
            declared = new URL(landing, raw); // 상대 경로 canonical 대응
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
        String protocol = lower(declared.getProtocol());
        if (!protocol.equals("http") && !protocol.equals("https")) {
            return Optional.empty();
        }
        // 같은 플랫폼의 다른 사용자 페이지로 합쳐지지 않게 호스트로 비교한다.
        if (!sameHostIgnoringDevicePrefix(landing.getHost(), declared.getHost())) {
            return Optional.empty();
        }
        // 모든 글의 canonical을 홈으로 둔 사이트
        if (isRoot(declared)) {
            return Optional.empty();
        }
        return Optional.of(finish(declared.toString()));
    }

    // canonical, og:url로 합쳐지지 않는 같은 콘텐츠 주소를 맞춘다.
    public static String applySiteRules(String url) {
        URL parsed = parse(url);
        if (parsed == null) {
            return url;
        }
        String host = lower(parsed.getHost());
        // 네이버 뉴스: /mnews/article/와 /article/은 같은 기사다.
        if (host.equals("n.news.naver.com") && parsed.getPath().startsWith("/mnews/article/")) {
            return url.replaceFirst("/mnews/article/", "/article/");
        }
        return url;
    }

    private static String finish(String url) {
        return UrlUtils.normalizeUrl(applySiteRules(url));
    }

    static boolean sameSite(String a, String b) {
        String siteA = registrableDomain(hostOf(a));
        String siteB = registrableDomain(hostOf(b));
        return siteA != null && siteA.equals(siteB);
    }

    private static String registrableDomain(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        try {
            if (InternetDomainName.isValid(host)) {
                InternetDomainName idn = InternetDomainName.from(host);
                if (idn.hasRegistrySuffix() && !idn.isRegistrySuffix()) {
                    return idn.topDomainUnderRegistrySuffix().toString();
                }
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            // 계산할 수 없으면 호스트 그대로 비교
        }
        return host;
    }

    static boolean sameHostIgnoringDevicePrefix(String a, String b) {
        return stripDevicePrefix(lower(a)).equals(stripDevicePrefix(lower(b)));
    }

    private static String stripDevicePrefix(String host) {
        for (String prefix : DEVICE_HOST_PREFIXES) {
            if (host.startsWith(prefix)) {
                return host.substring(prefix.length());
            }
        }
        return host;
    }

    public static boolean collapsedToRoot(String inputUrl, String candidate) {
        URL in = parse(inputUrl);
        URL out = parse(candidate);
        if (in == null || out == null) {
            return false;
        }
        boolean inputHasPath =
                in.getPath() != null && !in.getPath().isEmpty() && !in.getPath().equals("/");
        return inputHasPath && isRoot(out);
    }

    // 쿼리로 콘텐츠를 구분하는 주소는 홈이 아니다.
    private static boolean isRoot(URL url) {
        boolean rootPath =
                url.getPath() == null || url.getPath().isEmpty() || url.getPath().equals("/");
        boolean noQuery = url.getQuery() == null || url.getQuery().isEmpty();
        return rootPath && noQuery;
    }

    private static String attr(Element element, String name) {
        return element == null ? null : element.attr(name);
    }

    private static String hostOf(String url) {
        URL parsed = parse(url);
        return parsed == null ? null : lower(parsed.getHost());
    }

    private static URL parse(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    private static String lower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }
}
