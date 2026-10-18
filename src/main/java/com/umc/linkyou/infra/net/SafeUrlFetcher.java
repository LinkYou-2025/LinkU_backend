package com.umc.linkyou.infra.net;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

// SsrfGuard 검증을 거친 URL로만 요청을 보내고, 리다이렉트는 hop마다 재검증하며 수동으로 따라가는 단일 HTTP 창구.
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeUrlFetcher {

    private final SsrfGuard ssrfGuard;

    private static final int[] REDIRECT_CODES = {301, 302, 303, 307, 308};

    // 리다이렉트를 끝까지 따라간 최종 목적지의 HttpURLConnection을 반환한다.
    public HttpURLConnection openConnection(String urlStr, String userAgent, int connectTimeoutMs, int readTimeoutMs)
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
                conn.disconnect();
                throw e;
            }
            if (isRedirect(status)) {
                String location = conn.getHeaderField("Location");
                conn.disconnect();
                if (location == null || location.isBlank()) {
                    throw new SsrfGuard.BlockedException("리다이렉트 응답에 Location 헤더 없음: " + current);
                }
                current = new URL(url, location).toString();
                continue;
            }
            return conn;
        }
        throw new SsrfGuard.BlockedException("리다이렉트 허용 횟수(" + SsrfGuard.MAX_REDIRECTS + ") 초과: " + urlStr);
    }

    // 리다이렉트를 따라가 HTML Document를 가져온다. 4xx/5xx는 IOException.
    public Document fetchDocument(String urlStr, String userAgent, int timeoutMs) throws IOException {
        String current = urlStr;
        for (int hop = 0; hop <= SsrfGuard.MAX_REDIRECTS; hop++) {
            URL url = ssrfGuard.validate(current);

            Connection.Response response = Jsoup.connect(url.toString())
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
                current = new URL(url, location).toString();
                continue;
            }
            if (status >= 400) {
                throw new IOException("HTTP " + status + " for " + current);
            }
            return response.parse();
        }
        throw new SsrfGuard.BlockedException("리다이렉트 허용 횟수(" + SsrfGuard.MAX_REDIRECTS + ") 초과: " + urlStr);
    }

    // URL이 2xx~3xx로 응답하는지 확인한다.
    public boolean isReachable(String urlStr) {
        try {
            HttpURLConnection conn = openConnection(urlStr, DEFAULT_USER_AGENT, 3000, 3000);
            int status = conn.getResponseCode();
            conn.disconnect();
            return status >= 200 && status < 400;
        } catch (Exception e) {
            log.warn("[URL 도달 가능 여부 확인 실패] {}: {}", urlStr, e.getMessage());
            return false;
        }
    }

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
