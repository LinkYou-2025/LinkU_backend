package com.umc.linkyou.infra.net;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// 외부 요청은 SsrfGuard 검증을 거쳐 연다.
// 리다이렉트로 검증을 우회하지 못하게 자동 리다이렉트를 끄고 hop마다 다시 검증한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeUrlFetcher {

    private final SsrfGuard ssrfGuard;

    private static final int[] REDIRECT_CODES = {301, 302, 303, 307, 308};

    // 반환된 커넥션은 리다이렉트를 모두 따라간 최종 목적지에 연결돼 있다.
    public HttpURLConnection openConnection(
            String urlStr, String userAgent, int connectTimeoutMs, int readTimeoutMs)
            throws IOException {
        return openConnection(
                urlStr, userAgent, connectTimeoutMs, readTimeoutMs, new ArrayList<>());
    }

    private HttpURLConnection openConnection(
            String urlStr,
            String userAgent,
            int connectTimeoutMs,
            int readTimeoutMs,
            List<RedirectHop> hops)
            throws IOException {
        String current = urlStr;
        for (int hop = 0; hop <= SsrfGuard.MAX_REDIRECTS; hop++) {
            URL url = ssrfGuard.validate(current);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", userAgent);
            conn.setConnectTimeout(connectTimeoutMs);
            conn.setReadTimeout(readTimeoutMs);

            int status;
            try {
                status = conn.getResponseCode();
            } catch (IOException e) {
                // 실패한 hop의 커넥션이 새지 않게 닫고 다시 던진다.
                conn.disconnect();
                throw e;
            }
            if (isRedirect(status)) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isBlank()) {
                    throw new SsrfGuard.BlockedException("리다이렉트 응답에 Location 헤더 없음: " + current);
                }
                String next = new URL(url, location).toString();
                hops.add(new RedirectHop(current, status, next));
                current = next;
                continue;
            }
            return conn;
        }
        throw new SsrfGuard.BlockedException(
                "리다이렉트 허용 횟수(" + SsrfGuard.MAX_REDIRECTS + ") 초과: " + urlStr);
    }

    public Document fetchDocument(String urlStr, String userAgent, int timeoutMs)
            throws IOException {
        return fetch(urlStr, userAgent, timeoutMs, false).document();
    }

    // fetchDocument와 달리 지연 0초 meta refresh도 따라가고, 거쳐간 이동을 hops에 담는다.
    public FetchedPage fetchPage(String urlStr, String userAgent, int timeoutMs)
            throws IOException {
        return fetch(urlStr, userAgent, timeoutMs, true);
    }

    private FetchedPage fetch(
            String urlStr, String userAgent, int timeoutMs, boolean followMetaRefresh)
            throws IOException {
        String current = urlStr;
        List<RedirectHop> hops = new ArrayList<>();
        for (int hop = 0; hop <= SsrfGuard.MAX_REDIRECTS; hop++) {
            URL url = ssrfGuard.validate(current);

            Connection.Response response =
                    Jsoup.connect(url.toString())
                            .userAgent(userAgent)
                            .timeout(timeoutMs)
                            .followRedirects(false)
                            .ignoreHttpErrors(true)
                            .execute();

            int status = response.statusCode();
            if (isRedirect(status)) {
                String location = response.header("Location");
                if (location == null || location.isBlank()) {
                    throw new SsrfGuard.BlockedException("리다이렉트 응답에 Location 헤더 없음: " + current);
                }
                String next = new URL(url, location).toString();
                hops.add(new RedirectHop(current, status, next));
                current = next;
                continue;
            }
            if (status >= 400) {
                // Jsoup get()처럼 4xx, 5xx는 예외로 던진다.
                throw new IOException("HTTP " + status + " for " + current);
            }
            Document doc = response.parse();
            if (followMetaRefresh) {
                Optional<String> refreshTarget = metaRefreshTarget(doc, url);
                if (refreshTarget.isPresent()) {
                    hops.add(new RedirectHop(current, META_REFRESH_STATUS, refreshTarget.get()));
                    current = refreshTarget.get();
                    continue;
                }
            }
            return new FetchedPage(doc, current, List.copyOf(hops));
        }
        throw new SsrfGuard.BlockedException(
                "리다이렉트 허용 횟수(" + SsrfGuard.MAX_REDIRECTS + ") 초과: " + urlStr);
    }

    // 지연 0초만 리다이렉트로 본다. 지연이 있으면 사용자에게 보여주려는 안내 페이지다.
    static Optional<String> metaRefreshTarget(Document doc, URL base) {
        if (doc.head() == null) {
            return Optional.empty();
        }
        // body 안의 meta refresh는 안내용이라 head만 본다.
        for (Element meta : doc.head().children()) {
            if (!meta.tagName().equalsIgnoreCase("meta") || !meta.hasAttr("http-equiv")) {
                continue;
            }
            if (!"refresh".equalsIgnoreCase(meta.attr("http-equiv").trim())) {
                continue;
            }
            String content = meta.attr("content");
            int sep = content.indexOf(';');
            if (sep < 0) {
                continue;
            }
            try {
                if (Double.parseDouble(content.substring(0, sep).trim()) > 0) {
                    continue;
                }
            } catch (NumberFormatException e) {
                continue;
            }
            String rest = content.substring(sep + 1).trim();
            if (rest.regionMatches(true, 0, "url=", 0, 4)) {
                rest = rest.substring(4).trim();
            }
            // URL을 감싼 따옴표를 뗀다.
            rest = rest.replaceAll("^['\"]|['\"]$", "");
            if (rest.isEmpty()) {
                continue;
            }
            try {
                return Optional.of(new URL(base, rest).toString());
            } catch (MalformedURLException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    public boolean isReachable(String urlStr) {
        return probe(urlStr).isPresent();
    }

    // 최종 URL은 이어서 크롤링할 때 리다이렉트를 다시 타지 않으려고 돌려준다.
    public Optional<ProbeResult> probe(String urlStr) {
        HttpURLConnection conn = null;
        List<RedirectHop> hops = new ArrayList<>();
        try {
            conn = openConnection(urlStr, DEFAULT_USER_AGENT, 3000, 3000, hops);
            int status = conn.getResponseCode();
            if (status >= 200 && status < 400) {
                return Optional.of(new ProbeResult(conn.getURL().toString(), List.copyOf(hops)));
            }
            return Optional.empty();
        } catch (Exception e) {
            log.warn("[URL 도달 가능 여부 확인 실패] {}: {}", urlStr, e.getMessage());
            return Optional.empty();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    public record RedirectHop(String fromUrl, int status, String toUrl) {}

    public record ProbeResult(String finalUrl, List<RedirectHop> hops) {}

    public record FetchedPage(Document document, String finalUrl, List<RedirectHop> hops) {}

    // meta refresh hop의 상태 코드. 임시 이동으로 취급된다.
    public static final int META_REFRESH_STATUS = 0;

    // TODO: DEFAULT_USER_AGENT에 브라우저 UA를 쓰는 이유를 서원에게 확인 후 주석 추가
    private static final String DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0 Safari/537.36";

    private boolean isRedirect(int status) {
        for (int code : REDIRECT_CODES) {
            if (code == status) {
                return true;
            }
        }
        return false;
    }
}
