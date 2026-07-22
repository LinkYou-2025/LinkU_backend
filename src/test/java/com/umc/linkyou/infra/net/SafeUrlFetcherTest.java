package com.umc.linkyou.infra.net;

import com.sun.net.httpserver.HttpServer;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SafeUrlFetcher 테스트")
class SafeUrlFetcherTest {

    // 로컬 HTTP 서버(127.0.0.1)로 테스트해야 하므로 loopback을 허용한 SsrfGuard를 쓴다.
    private final SafeUrlFetcher fetcher = new SafeUrlFetcher(SsrfGuard.forTesting(true));

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);

        server.createContext("/final", exchange -> {
            byte[] body = "<html><body>final page</body></html>".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/start", exchange -> {
            exchange.getResponseHeaders().add("Location", "/final");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        server.createContext("/loop", exchange -> {
            exchange.getResponseHeaders().add("Location", "/loop");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        server.createContext("/toBlocked", exchange -> {
            exchange.getResponseHeaders().add("Location", "http://169.254.169.254/");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        server.createContext("/notfound", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });

        server.start();
        return "http://localhost:" + server.getAddress().getPort();
    }

    @Nested
    @DisplayName("fetchDocument")
    class FetchDocument {

        @Test
        @DisplayName("리다이렉트가 없으면 바로 문서를 가져온다")
        void 리다이렉트_없으면_바로_문서를_가져온다() throws Exception {
            String base = startServer();

            Document doc = fetcher.fetchDocument(base + "/final", "Mozilla/5.0", 5000);

            assertThat(doc.body().text()).isEqualTo("final page");
        }

        @Test
        @DisplayName("리다이렉트를 따라가서 최종 문서를 가져온다")
        void 리다이렉트를_따라가서_최종_문서를_가져온다() throws Exception {
            String base = startServer();

            Document doc = fetcher.fetchDocument(base + "/start", "Mozilla/5.0", 5000);

            assertThat(doc.body().text()).isEqualTo("final page");
        }

        @Test
        @DisplayName("리다이렉트 목적지가 차단 대상(링크로컬)이면 예외를 던진다")
        void 리다이렉트_목적지가_차단대상이면_예외를_던진다() throws Exception {
            String base = startServer();

            assertThatThrownBy(() -> fetcher.fetchDocument(base + "/toBlocked", "Mozilla/5.0", 5000))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
        }

        @Test
        @DisplayName("리다이렉트가 허용 횟수를 넘으면 예외를 던진다")
        void 리다이렉트_허용횟수_초과시_예외를_던진다() throws Exception {
            String base = startServer();

            assertThatThrownBy(() -> fetcher.fetchDocument(base + "/loop", "Mozilla/5.0", 5000))
                    .isInstanceOf(SsrfGuard.BlockedException.class);
        }
    }

    @Nested
    @DisplayName("openConnection / isReachable")
    class OpenConnectionAndReachable {

        @Test
        @DisplayName("정상 응답이면 isReachable은 true를 반환한다")
        void 정상_응답이면_reachable_true() throws Exception {
            String base = startServer();

            assertThat(fetcher.isReachable(base + "/final")).isTrue();
        }

        @Test
        @DisplayName("404 응답이면 isReachable은 false를 반환한다")
        void 응답코드가_404면_reachable_false() throws Exception {
            String base = startServer();

            assertThat(fetcher.isReachable(base + "/notfound")).isFalse();
        }

        @Test
        @DisplayName("차단 대상 URL이면 isReachable은 예외 없이 false를 반환한다")
        void 차단대상이면_reachable_false() {
            assertThat(fetcher.isReachable("http://169.254.169.254/")).isFalse();
        }

        @Test
        @DisplayName("리다이렉트를 따라간 뒤 최종 연결을 반환한다")
        void 리다이렉트를_따라간_뒤_최종_연결을_반환한다() throws Exception {
            String base = startServer();

            HttpURLConnection conn = fetcher.openConnection(base + "/start", "Mozilla/5.0", 5000, 5000);

            assertThat(conn.getResponseCode()).isEqualTo(200);
            conn.disconnect();
        }
    }
}
