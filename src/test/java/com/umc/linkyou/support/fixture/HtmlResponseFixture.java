package com.umc.linkyou.support.fixture;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class HtmlResponseFixture {
    public static final String UNDERSCORE_URL = "https://tew_books.gitlab.io/post";
    public static final String UNDERSCORE_HOST = "tew_books.gitlab.io";
    public static final String PLATFORM_HOST = "gitlab.io";
    public static final String HTML = "<html><head><title>테스트 제목</title></head><body>압축 본문</body></html>";

    private HtmlResponseFixture() {}

    public static byte[] htmlBytes() {
        return HTML.getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] gzipHtml() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(htmlBytes());
        }
        return bytes.toByteArray();
    }

    // 데이터는 계속 도착하지만 전체 다운로드가 지연되는 서버를 재현한다.
    public static InputStream tricklingHtml() {
        return new ByteArrayInputStream(htmlBytes()) {
            @Override
            public int read(byte[] bytes, int offset, int length) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return -1;
                }
                return super.read(bytes, offset, Math.min(length, 1));
            }
        };
    }

    public static HttpURLConnection response(String contentType, String encoding, byte[] body) throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getResponseCode()).thenReturn(200);
        when(connection.getContentType()).thenReturn(contentType);
        when(connection.getContentEncoding()).thenReturn(encoding);
        when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(body));
        when(connection.getURL()).thenReturn(new URL(UNDERSCORE_URL));
        return connection;
    }

    public static URL url(HttpURLConnection connection) throws IOException {
        return new URL(null, UNDERSCORE_URL, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) {
                return connection;
            }
        });
    }
}
