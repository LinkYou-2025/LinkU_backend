package com.umc.linkyou.infra.net;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.UnsupportedMimeTypeException;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import org.springframework.http.MediaType;

// SsrfGuard 검증을 거친 URL로만 요청을 보내고, 리다이렉트는 hop마다 재검증하며 수동으로 따라가는 단일 HTTP 창구.
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeUrlFetcher {

    private final SsrfGuard ssrfGuard;

    private static final int[] REDIRECT_CODES = {301, 302, 303, 307, 308};
    private static final int MAX_DOCUMENT_TIMEOUT_MS = 3000;

    // 리다이렉트를 끝까지 따라간 최종 목적지의 HttpURLConnection을 반환한다.
    public HttpURLConnection openConnection(String urlStr, String userAgent, int connectTimeoutMs, int readTimeoutMs)
            throws IOException {
        return openConnection(urlStr, userAgent, connectTimeoutMs, readTimeoutMs, SsrfGuard.MAX_REDIRECTS, false, 0);
    }

    private HttpURLConnection openConnection(String urlStr, String userAgent, int connectTimeoutMs,
            int readTimeoutMs, int remainingRedirects, boolean acceptGzip, long deadlineNanos) throws IOException {
        String current = urlStr;
        for (int hop = 0; hop <= remainingRedirects; hop++) {
            URL url = ssrfGuard.validate(current);
            int remaining = acceptGzip ? remainingMillis(deadlineNanos) : Integer.MAX_VALUE;
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", userAgent);
            // 압축 해제를 수행하는 HTML 요청에만 압축 응답을 요청한다.
            if (acceptGzip) conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setConnectTimeout(Math.min(connectTimeoutMs, remaining));
            conn.setReadTimeout(Math.min(readTimeoutMs, remaining));
            if (conn instanceof HttpsURLConnection https && url.getHost().contains("_")) {
                // 연결 단위로만 적용한다. 기본 TrustManager의 인증서 신뢰 체인 검증은 유지한다.
                https.setHostnameVerifier(SafeUrlFetcher::verifyUnderscoreHostname);
            }

            int status;
            try {
                status = conn.getResponseCode();
                if (acceptGzip) remainingMillis(deadlineNanos);
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
        // 모든 리다이렉트가 같은 시간 예산을 사용한다. DNS 조회 자체는 JVM/OS 정책을 따른다.
        int budgetMs = timeoutMs > 0 ? Math.min(timeoutMs, MAX_DOCUMENT_TIMEOUT_MS) : MAX_DOCUMENT_TIMEOUT_MS;
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(budgetMs);
        String current = urlStr;
        for (int hop = 0; hop <= SsrfGuard.MAX_REDIRECTS; hop++) {
            if (new URL(current).getHost().contains("_")) {
                return fetchUnderscoreDocument(current, userAgent, budgetMs,
                        SsrfGuard.MAX_REDIRECTS - hop, deadlineNanos);
            }
            URL url = ssrfGuard.validate(current);

            Connection.Response response = Jsoup.connect(url.toString())
                    .userAgent(userAgent)
                    .timeout(remainingMillis(deadlineNanos))
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .execute();

            int status = response.statusCode();
            if (isRedirect(status)) {
                String location = response.header("Location");
                response.bodyStream().close();
                remainingMillis(deadlineNanos);
                if (location == null || location.isBlank()) {
                    throw new SsrfGuard.BlockedException("리다이렉트 응답에 Location 헤더 없음: " + current);
                }
                current = new URL(url, location).toString();
                continue;
            }
            if (status >= 400) {
                response.bodyStream().close();
                throw new IOException("HTTP " + status + " for " + current);
            }
            Document document = response.parse();
            remainingMillis(deadlineNanos);
            return document;
        }
        throw new SsrfGuard.BlockedException("리다이렉트 허용 횟수(" + SsrfGuard.MAX_REDIRECTS + ") 초과: " + urlStr);
    }

    private Document fetchUnderscoreDocument(
            String url, String userAgent, int timeoutMs, int remainingRedirects, long deadlineNanos) throws IOException {
        HttpURLConnection conn = openConnection(url, userAgent, timeoutMs, timeoutMs, remainingRedirects, true, deadlineNanos);
        try {
            int status = conn.getResponseCode();
            if (status >= 400) throw new IOException("HTTP " + status + " for " + conn.getURL());
            String charset = null;
            String contentType = conn.getContentType();
            if (contentType != null) {
                try {
                    MediaType mediaType = MediaType.parseMediaType(contentType);
                    // Jsoup처럼 텍스트/XML만 파싱하고 PDF·이미지 등 바이너리는 거부한다.
                    if (!"text".equalsIgnoreCase(mediaType.getType())
                            && !("application".equalsIgnoreCase(mediaType.getType())
                            && ("xml".equalsIgnoreCase(mediaType.getSubtype())
                            || mediaType.getSubtype().toLowerCase(Locale.ROOT).endsWith("+xml")))) {
                        throw new UnsupportedMimeTypeException("Unsupported content type", contentType,
                                conn.getURL().toString());
                    }
                    var parsedCharset = mediaType.getCharset();
                    if (parsedCharset != null) charset = parsedCharset.name();
                } catch (IllegalArgumentException e) {
                    throw new UnsupportedMimeTypeException("Invalid content type", contentType,
                            conn.getURL().toString());
                }
            }
            remainingMillis(deadlineNanos);
            try (InputStream raw = deadlineStream(conn.getInputStream(), deadlineNanos);
                    InputStream body = decodedStream(raw, conn.getContentEncoding())) {
                // Jsoup 기본 응답 본문 제한과 동일하게 압축 해제 후 2MiB까지만 읽는다.
                byte[] bytes = body.readNBytes(2 * 1024 * 1024);
                remainingMillis(deadlineNanos);
                Document document = Jsoup.parse(new ByteArrayInputStream(bytes), charset, conn.getURL().toString());
                remainingMillis(deadlineNanos);
                return document;
            }
        } finally {
            conn.disconnect();
        }
    }

    private static int remainingMillis(long deadlineNanos) throws SocketTimeoutException {
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) throw new SocketTimeoutException("HTML download deadline exceeded");
        // 0은 무제한 대기를 의미하므로 최소 1ms를 사용한다.
        return (int) Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining));
    }

    // 이미 시작된 소켓 읽기는 연결 시 설정한 read timeout까지 기다릴 수 있다.
    private InputStream deadlineStream(InputStream input, long deadlineNanos) {
        return new FilterInputStream(input) {
            @Override
            public int read() throws IOException {
                remainingMillis(deadlineNanos);
                int value = in.read();
                remainingMillis(deadlineNanos);
                return value;
            }

            @Override
            public int read(byte[] bytes, int offset, int length) throws IOException {
                remainingMillis(deadlineNanos);
                int count = in.read(bytes, offset, length);
                remainingMillis(deadlineNanos);
                return count;
            }
        };
    }

    private InputStream decodedStream(InputStream input, String encoding) throws IOException {
        if ("gzip".equalsIgnoreCase(encoding)) return new GZIPInputStream(input);
        if ("deflate".equalsIgnoreCase(encoding)) return new InflaterInputStream(input);
        return input;
    }

    // 신뢰 체인 검증을 통과한 인증서의 DNS SAN만 대조한다. CN 폴백은 허용하지 않는다.
    static boolean verifyUnderscoreHostname(String hostname, SSLSession session) {
        if (hostname == null || !hostname.contains("_")
                || !hostname.matches("[a-zA-Z0-9_.-]+")) {
            return false;
        }
        try {
            Certificate[] certificates = session.getPeerCertificates();
            if (certificates.length == 0 || !(certificates[0] instanceof X509Certificate leaf)) {
                return false;
            }
            Collection<List<?>> names = leaf.getSubjectAlternativeNames();
            if (names == null) return false;
            for (List<?> name : names) {
                if (name.size() >= 2 && Integer.valueOf(2).equals(name.get(0))
                        && name.get(1) instanceof String dnsName && matchesDnsName(hostname, dnsName)) {
                    return true;
                }
            }
        } catch (SSLPeerUnverifiedException | CertificateParsingException e) {
            return false;
        }
        return false;
    }

    private static boolean matchesDnsName(String hostname, String dnsName) {
        String host = hostname.toLowerCase(Locale.ROOT);
        String dns = dnsName.toLowerCase(Locale.ROOT);
        if (!dns.startsWith("*.")) return host.equals(dns);

        // 와일드카드는 가장 왼쪽의 한 라벨만 허용한다.
        String suffix = dns.substring(2);
        int firstDot = host.indexOf('.');
        return firstDot > 0 && suffix.indexOf('.') > 0 && !suffix.contains("*")
                && host.substring(firstDot + 1).equals(suffix);
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
