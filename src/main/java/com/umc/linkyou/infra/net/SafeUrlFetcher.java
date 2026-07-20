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

/**
 * {@link SsrfGuard}로 검증한 URL에 대해서만 실제 네트워크 요청을 여는 단일 창구.
 * robots.txt 조회, 제목/본문/이미지 크롤링, URL 도달 가능 여부 확인이 전부 이 클래스를 거친다.
 *
 * 자동 리다이렉트는 끄고, 매 hop마다 Location 헤더를 다시 {@link SsrfGuard}로 검증한 뒤 수동으로 따라간다.
 * 원본 URL만 검증하고 리다이렉트 목적지는 그냥 따라가 버리면 allowlist 검증 자체가 우회되기 때문이다.
 * 최대 {@link SsrfGuard#MAX_REDIRECTS}회까지만 허용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeUrlFetcher {

    private final SsrfGuard ssrfGuard;

    private static final int[] REDIRECT_CODES = {301, 302, 303, 307, 308};

    // robots.txt 조회, URL 도달 가능 여부 확인 등에서 쓰는 raw HttpURLConnection.
    // 반환된 connection은 이미 최종 목적지(리다이렉트를 다 따라간 뒤)에 연결된 상태다.
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
                // getResponseCode() 자체가 실패하면(타임아웃 등) 이 hop의 커넥션이 안 닫힌 채로
                // 새지 않도록 여기서 바로 닫고 다시 던진다.
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

    // 제목/본문/이미지 크롤링에서 쓰는 Jsoup 기반 Document 조회.
    public Document fetchDocument(String urlStr, String userAgent, int timeoutMs) throws IOException {
        String current = urlStr;
        for (int hop = 0; hop <= SsrfGuard.MAX_REDIRECTS; hop++) {
            URL url = ssrfGuard.validate(current);

            Connection.Response response = Jsoup.connect(url.toString())
                    .userAgent(userAgent)
                    .timeout(timeoutMs)
                    .followRedirects(false)
                    .ignoreHttpErrors(true) // 상태코드를 직접 보고 리다이렉트/에러를 판단하기 위함
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
                // 기존 Jsoup.connect().get() 기본 동작(4xx/5xx에서 예외)과 동일하게 맞춘다.
                throw new IOException("HTTP " + status + " for " + current);
            }
            return response.parse();
        }
        throw new SsrfGuard.BlockedException("리다이렉트 허용 횟수(" + SsrfGuard.MAX_REDIRECTS + ") 초과: " + urlStr);
    }

    // URL이 실제로 응답 가능한 상태인지(2xx~3xx)만 확인한다. LinkuCreateService의 validUrl 필드에 쓰인다.
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
