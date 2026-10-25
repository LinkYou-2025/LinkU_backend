package com.umc.linkyou.infra.net;

import com.sun.net.httpserver.HttpServer;
import com.umc.linkyou.support.fixture.TlsSessionFixture;
import com.umc.linkyou.support.fixture.HtmlResponseFixture;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import org.jsoup.nodes.Document;
import org.jsoup.UnsupportedMimeTypeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
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

    @Nested
    @DisplayName("밑줄 호스트 HTML 응답 처리")
    class UnderscoreHtmlResponse {
        private Document fetchResponse(HttpURLConnection connection) throws Exception {
            return fetchResponse(connection, 5000);
        }

        private Document fetchResponse(HttpURLConnection connection, int timeoutMs) throws Exception {
            SsrfGuard guard = mock(SsrfGuard.class);
            when(guard.validate(HtmlResponseFixture.UNDERSCORE_URL))
                    .thenReturn(HtmlResponseFixture.url(connection));
            return new SafeUrlFetcher(guard).fetchDocument(HtmlResponseFixture.UNDERSCORE_URL,
                    "Mozilla/5.0", timeoutMs);
        }

        @Nested
        @DisplayName("성공 시 본문을 파싱한다")
        class Success {
            @Test
            @DisplayName("gzip 응답 시 압축을 해제하여 한글 본문을 반환한다")
            void gzip_응답_시_압축을_해제하여_한글_본문을_반환한다() throws Exception {
                HttpURLConnection connection = HtmlResponseFixture.response("text/html; charset=UTF-8",
                        "gzip", HtmlResponseFixture.gzipHtml());

                Document document = fetchResponse(connection);

                assertThat(document.title()).isEqualTo("테스트 제목");
                assertThat(document.body().text()).isEqualTo("압축 본문");
                verify(connection).setRequestProperty("Accept-Encoding", "gzip");
                verify(connection).disconnect();
            }

            @ParameterizedTest
            @ValueSource(strings = {"text/html; charset=UTF-8", "application/xhtml+xml; charset=UTF-8"})
            @DisplayName("비압축 HTML 또는 XHTML 응답 시 본문을 반환한다")
            void 비압축_HTML_또는_XHTML_응답_시_본문을_반환한다(String contentType) throws Exception {
                HttpURLConnection connection = HtmlResponseFixture.response(contentType, null,
                        HtmlResponseFixture.htmlBytes());

                assertThat(fetchResponse(connection).body().text()).isEqualTo("압축 본문");
            }
        }

        @Nested
        @DisplayName("실패 시 본문을 읽지 않는다")
        class Failure {
            @Test
            @DisplayName("데이터가 조금씩 도착해도 전체 시간 초과 시 다운로드를 중단한다")
            void 데이터가_조금씩_도착해도_전체_시간_초과_시_다운로드를_중단한다() throws Exception {
                HttpURLConnection connection = HtmlResponseFixture.response("text/html; charset=UTF-8", null,
                        HtmlResponseFixture.htmlBytes());
                when(connection.getInputStream()).thenReturn(HtmlResponseFixture.tricklingHtml());

                assertThatThrownBy(() -> fetchResponse(connection, 100))
                        .isInstanceOf(SocketTimeoutException.class)
                        .hasMessage("HTML download deadline exceeded");
                verify(connection).disconnect();
            }

            @ParameterizedTest
            @ValueSource(strings = {"application/pdf", "image/png", "application/octet-stream"})
            @DisplayName("바이너리 응답 시 본문을 읽기 전에 거부한다")
            void 바이너리_응답_시_본문을_읽기_전에_거부한다(String contentType) throws Exception {
                HttpURLConnection connection = HtmlResponseFixture.response(contentType, null,
                        HtmlResponseFixture.htmlBytes());

                assertThatThrownBy(() -> fetchResponse(connection))
                        .isInstanceOfSatisfying(UnsupportedMimeTypeException.class,
                                e -> assertThat(e.getMimeType()).isEqualTo(contentType));
                verify(connection, never()).getInputStream();
                verify(connection).disconnect();
            }
        }
    }

    @Nested
    @DisplayName("밑줄 호스트 인증서 검증")
    class UnderscoreHostnameValidation {
        @Nested
        @DisplayName("성공 시 인증서의 DNS 이름을 확인한다")
        class Success {
            @Test
            @DisplayName("호스트와 SAN이 일치할 시 허용한다")
            void 호스트와_SAN이_일치할_시_허용한다() throws Exception {
                assertThat(SafeUrlFetcher.verifyUnderscoreHostname("tew_books.gitlab.io",
                        TlsSessionFixture.withDnsSan("tew_books.gitlab.io"))).isTrue();
            }

            @Test
            @DisplayName("한 라벨의 와일드카드와 일치할 시 대소문자와 무관하게 허용한다")
            void 한_라벨의_와일드카드와_일치할_시_대소문자와_무관하게_허용한다() throws Exception {
                assertThat(SafeUrlFetcher.verifyUnderscoreHostname("TEW_BOOKS.gitlab.io",
                        TlsSessionFixture.withDnsSan("*.GITLAB.IO"))).isTrue();
            }
        }

        @Nested
        @DisplayName("실패 시 호스트 검증을 거부한다")
        class Failure {
            @Test
            @DisplayName("다른 도메인의 인증서일 시 거부한다")
            void 다른_도메인의_인증서일_시_거부한다() throws Exception {
                assertThat(SafeUrlFetcher.verifyUnderscoreHostname("tew_books.gitlab.io",
                        TlsSessionFixture.withDnsSan("*.example.com"))).isFalse();
            }

            @Test
            @DisplayName("와일드카드가 여러 라벨에 걸칠 시 거부한다")
            void 와일드카드가_여러_라벨에_걸칠_시_거부한다() throws Exception {
                assertThat(SafeUrlFetcher.verifyUnderscoreHostname("a.tew_books.gitlab.io",
                        TlsSessionFixture.withDnsSan("*.gitlab.io"))).isFalse();
            }

            @Test
            @DisplayName("와일드카드가 도메인 경계를 넘거나 부분 라벨일 시 거부한다")
            void 와일드카드가_도메인_경계를_넘거나_부분_라벨일_시_거부한다() throws Exception {
                assertThat(SafeUrlFetcher.verifyUnderscoreHostname("tew_books.evilgitlab.io",
                        TlsSessionFixture.withDnsSan("*.gitlab.io"))).isFalse();
                assertThat(SafeUrlFetcher.verifyUnderscoreHostname("tew_books.gitlab.io",
                        TlsSessionFixture.withDnsSan("tew*.gitlab.io"))).isFalse();
            }

            @Test
            @DisplayName("DNS SAN이 없을 시 CN으로 대체하지 않고 거부한다")
            void DNS_SAN이_없을_시_CN으로_대체하지_않고_거부한다() throws Exception {
                assertThat(SafeUrlFetcher.verifyUnderscoreHostname("tew_books.gitlab.io",
                        TlsSessionFixture.withoutDnsSan())).isFalse();
            }

            @Test
            @DisplayName("피어 인증서를 확인할 수 없을 시 거부한다")
            void 피어_인증서를_확인할_수_없을_시_거부한다() throws Exception {
                SSLSession session = mock(SSLSession.class);
                when(session.getPeerCertificates()).thenThrow(new SSLPeerUnverifiedException("unverified"));
                assertThat(SafeUrlFetcher.verifyUnderscoreHostname("tew_books.gitlab.io", session)).isFalse();
            }
        }
    }
}
