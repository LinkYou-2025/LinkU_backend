package com.umc.linkyou.infra.parser;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RobotsTxtChecker 테스트")
class RobotsTxtCheckerTest {

    private final RobotsTxtChecker checker = new RobotsTxtChecker();

    private List<RobotsTxtChecker.Rule> parse(String robotsTxt, String userAgent) throws Exception {
        return checker.parseRules(new BufferedReader(new StringReader(robotsTxt)), userAgent);
    }

    @Nested
    @DisplayName("경로 매칭 (matches)")
    class Matches {

        @Test
        @DisplayName("prefix 매칭 - 하위 경로까지 전부 매칭된다")
        void prefix_매칭() {
            assertThat(checker.matches("/private/a", "/private/")).isTrue();
            assertThat(checker.matches("/private", "/private/")).isFalse(); // 슬래시 없이는 매칭 안 됨
            assertThat(checker.matches("/publicity", "/public/")).isFalse(); // 다른 경로
        }

        @Test
        @DisplayName("대소문자를 구분한다")
        void 대소문자_구분() {
            assertThat(checker.matches("/Private/a", "/private/")).isFalse();
        }

        @Test
        @DisplayName("'$'는 정확히 그 지점에서 끝나는 경로만 매칭한다")
        void 달러_정확일치() {
            assertThat(checker.matches("/", "/$")).isTrue();
            assertThat(checker.matches("/anything", "/$")).isFalse();
        }

        @Test
        @DisplayName("'*'는 임의 문자열(0개 이상)과 매칭된다")
        void 와일드카드() {
            assertThat(checker.matches("/fish", "/fish*")).isTrue();
            assertThat(checker.matches("/fish.html", "/fish*")).isTrue();
            assertThat(checker.matches("/fish/salmon", "/fish*")).isTrue();
            assertThat(checker.matches("/catfish", "/fish*")).isFalse();
        }

        @Test
        @DisplayName("'*'와 '$'를 조합해 특정 확장자로 끝나는 경로만 매칭한다")
        void 와일드카드와_달러_조합() {
            assertThat(checker.matches("/index.php", "/*.php$")).isTrue();
            assertThat(checker.matches("/index.php5", "/*.php$")).isFalse();
            assertThat(checker.matches("/index.php?x=1", "/*.php$")).isFalse();
        }

        @Test
        @DisplayName("중간 와일드카드 + 접미사 패턴도 매칭한다")
        void 중간_와일드카드() {
            assertThat(checker.matches("/fish.php", "/fish*.php")).isTrue();
            assertThat(checker.matches("/fishheads/catfish.php?parameters", "/fish*.php")).isTrue();
            assertThat(checker.matches("/Fish.PHP", "/fish*.php")).isFalse();
        }
    }

    @Nested
    @DisplayName("규칙 파싱 및 판정 (parseRules + isPathAllowed)")
    class ParseAndDecide {

        @Test
        @DisplayName("기본 Disallow 규칙이 적용된다")
        void 기본_disallow() throws Exception {
            String robots = """
                    User-agent: *
                    Disallow: /private/
                    """;
            List<RobotsTxtChecker.Rule> rules = parse(robots, "*");

            assertThat(checker.isPathAllowed("/", rules)).isTrue();
            assertThat(checker.isPathAllowed("/about", rules)).isTrue();
            assertThat(checker.isPathAllowed("/private/", rules)).isFalse();
            assertThat(checker.isPathAllowed("/private/a", rules)).isFalse();
        }

        @Test
        @DisplayName("빈 Disallow 값은 무시되어 전체 허용된다")
        void 빈_disallow_전체허용() throws Exception {
            String robots = """
                    User-agent: *
                    Disallow:
                    """;
            List<RobotsTxtChecker.Rule> rules = parse(robots, "*");

            assertThat(rules).isEmpty();
            assertThat(checker.isPathAllowed("/anything", rules)).isTrue();
        }

        @Test
        @DisplayName("전체 차단 후 특정 디렉터리만 허용하는 패턴을 처리한다 (namu.wiki 실제 사례)")
        void 전체차단_일부허용() throws Exception {
            String robots = """
                    User-agent: *
                    Disallow: /
                    Allow: /public/
                    """;
            List<RobotsTxtChecker.Rule> rules = parse(robots, "*");

            assertThat(checker.isPathAllowed("/", rules)).isFalse();
            assertThat(checker.isPathAllowed("/login", rules)).isFalse();
            assertThat(checker.isPathAllowed("/public/", rules)).isTrue();
            assertThat(checker.isPathAllowed("/public/logo.png", rules)).isTrue();
            assertThat(checker.isPathAllowed("/publicity", rules)).isFalse(); // /public/ 과 다른 경로
        }

        @Test
        @DisplayName("길이가 같은 Allow/Disallow가 충돌하면 Allow(제한이 적은 규칙)가 우선한다")
        void 동률_충돌시_allow_우선() throws Exception {
            String robots = """
                    User-agent: *
                    Allow: /folder
                    Disallow: /folder
                    """;
            List<RobotsTxtChecker.Rule> rules = parse(robots, "*");

            assertThat(checker.isPathAllowed("/folder", rules)).isTrue();
            assertThat(checker.isPathAllowed("/folder123", rules)).isTrue();
        }

        @Test
        @DisplayName("길이가 같은 충돌은 선언 순서와 무관하게 항상 Allow가 이긴다")
        void 동률_충돌_순서무관() throws Exception {
            String robots = """
                    User-agent: *
                    Disallow: /folder
                    Allow: /folder
                    """;
            List<RobotsTxtChecker.Rule> rules = parse(robots, "*");

            assertThat(checker.isPathAllowed("/folder", rules)).isTrue();
        }

        @Test
        @DisplayName("인라인 주석은 값에서 제거된다")
        void 인라인_주석_제거() throws Exception {
            String robots = """
                    User-agent: *
                    Disallow: /secret # admin only
                    Allow: /open # public
                    """;
            List<RobotsTxtChecker.Rule> rules = parse(robots, "*");

            assertThat(rules).containsExactly(
                    new RobotsTxtChecker.Rule("/secret", false),
                    new RobotsTxtChecker.Rule("/open", true)
            );
            assertThat(checker.isPathAllowed("/secret", rules)).isFalse();
            assertThat(checker.isPathAllowed("/secret/page", rules)).isFalse();
            assertThat(checker.isPathAllowed("/open", rules)).isTrue();
        }

        @Test
        @DisplayName("주석으로만 이루어진 줄은 무시된다")
        void 주석전용_줄_무시() throws Exception {
            String robots = """
                    # 전체 정책 주석
                    User-agent: *
                    Disallow: /private/
                    """;
            List<RobotsTxtChecker.Rule> rules = parse(robots, "*");

            assertThat(checker.isPathAllowed("/private/x", rules)).isFalse();
        }

        @Test
        @DisplayName("특정 크롤러 전용 규칙은 다른 크롤러에 영향을 주지 않는다")
        void 특정_크롤러_전용_규칙() throws Exception {
            String robots = """
                    User-agent: Googlebot
                    Disallow: /nogoogle/

                    User-agent: *
                    Disallow:
                    """;

            List<RobotsTxtChecker.Rule> googlebotRules = parse(robots, "Googlebot");
            assertThat(checker.isPathAllowed("/nogoogle/a", googlebotRules)).isFalse();

            List<RobotsTxtChecker.Rule> otherBotRules = parse(robots, "MyBot");
            assertThat(checker.isPathAllowed("/nogoogle/a", otherBotRules)).isTrue();
        }

        @Test
        @DisplayName("[버그 수정] 연속된 User-agent 줄은 하나의 그룹으로 OR 결합된다")
        void 연속된_useragent_그룹결합() throws Exception {
            String robots = """
                    User-agent: Googlebot
                    User-agent: Storebot-Google
                    Allow: /cats
                    Disallow: /
                    """;

            List<RobotsTxtChecker.Rule> googlebotRules = parse(robots, "Googlebot");
            assertThat(checker.isPathAllowed("/cats", googlebotRules)).isTrue();
            assertThat(checker.isPathAllowed("/dogs", googlebotRules)).isFalse();

            List<RobotsTxtChecker.Rule> storebotRules = parse(robots, "Storebot-Google");
            assertThat(checker.isPathAllowed("/cats", storebotRules)).isTrue();
            assertThat(checker.isPathAllowed("/dogs", storebotRules)).isFalse();
        }

        @Test
        @DisplayName("[버그 수정] '*'가 다른 봇 이름보다 먼저 선언돼도 매칭이 유실되지 않는다")
        void 와일드카드_먼저선언되어도_그룹유실_안됨() throws Exception {
            // 이전 버그: 두 번째 User-agent 줄에서 무조건 덮어써서 '*' 매칭이 사라졌음
            String robots = """
                    User-agent: *
                    User-agent: SomeBot
                    Disallow: /x
                    """;

            List<RobotsTxtChecker.Rule> rules = parse(robots, "Mozilla/5.0");

            assertThat(rules).containsExactly(new RobotsTxtChecker.Rule("/x", false));
            assertThat(checker.isPathAllowed("/x/page", rules)).isFalse();
        }

        @Test
        @DisplayName("빈 줄로 그룹이 구분되면 이전 그룹의 매칭 상태가 초기화된다")
        void 빈줄로_그룹_초기화() throws Exception {
            String robots = """
                    User-agent: SomeBot
                    Disallow: /a/

                    User-agent: *
                    Disallow: /b/
                    """;

            List<RobotsTxtChecker.Rule> rules = parse(robots, "Mozilla/5.0");

            assertThat(rules).containsExactly(new RobotsTxtChecker.Rule("/b/", false));
            assertThat(checker.isPathAllowed("/a/x", rules)).isTrue(); // SomeBot 전용 규칙, 우리에겐 미적용
            assertThat(checker.isPathAllowed("/b/x", rules)).isFalse();
        }

        @Test
        @DisplayName("BOM이 첫 줄 앞에 있어도 정상 파싱된다")
        void BOM_제거() throws Exception {
            String robots = "﻿User-agent: *\nDisallow: /blocked/\n";
            List<RobotsTxtChecker.Rule> rules = parse(robots, "*");

            assertThat(checker.isPathAllowed("/blocked/1", rules)).isFalse();
            assertThat(checker.isPathAllowed("/ok", rules)).isTrue();
        }

        @Test
        @DisplayName("나무위키 실제 robots.txt로 /w/ 하위 문서는 허용된다")
        void 나무위키_실제_케이스() throws Exception {
            String robots = """
                    User-agent: *
                    Disallow: /
                    Allow: /$
                    Allow: /ads.txt
                    Allow: /w/
                    Allow: /history/
                    """;
            List<RobotsTxtChecker.Rule> rules = parse(robots, "Mozilla/5.0");

            assertThat(checker.isPathAllowed("/", rules)).isTrue();
            assertThat(checker.isPathAllowed("/w/TWS/%EC%9D%91%EC%9B%90%EB%B2%95", rules)).isTrue();
            assertThat(checker.isPathAllowed("/login", rules)).isFalse();
        }
    }

    @Nested
    @DisplayName("isAllowed 통합 테스트 (로컬 HTTP 서버)")
    class IsAllowedIntegration {

        private HttpServer server;

        @AfterEach
        void tearDown() {
            if (server != null) {
                server.stop(0);
            }
        }

        private String startServer(int statusCode, String body, String contentType) throws Exception {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/robots.txt", exchange -> {
                byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
                if (contentType != null) {
                    exchange.getResponseHeaders().add("Content-Type", contentType);
                }
                exchange.sendResponseHeaders(statusCode, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            });
            server.start();
            return "http://localhost:" + server.getAddress().getPort();
        }

        @Test
        @DisplayName("200 응답이면 robots.txt 규칙대로 판정한다")
        void 정상_응답_규칙적용() throws Exception {
            String base = startServer(200, """
                    User-agent: *
                    Disallow: /private/
                    """, "text/plain; charset=UTF-8");

            assertThat(checker.isAllowed(base + "/public/page", "Mozilla/5.0")).isTrue();
            assertThat(checker.isAllowed(base + "/private/page", "Mozilla/5.0")).isFalse();
        }

        @Test
        @DisplayName("robots.txt가 4xx면 기본 허용한다")
        void 사이트에_robots_txt_없으면_기본허용() throws Exception {
            String base = startServer(404, "", null);

            assertThat(checker.isAllowed(base + "/anything", "Mozilla/5.0")).isTrue();
        }

        @Test
        @DisplayName("[버그 수정] '$' 앵커 규칙은 쿼리스트링이 붙은 URL을 차단하지 않는다")
        void 달러앵커_쿼리스트링_미차단() throws Exception {
            String base = startServer(200, """
                    User-agent: *
                    Disallow: /*.xls$
                    """, "text/plain; charset=UTF-8");

            assertThat(checker.isAllowed(base + "/cats.xls", "Mozilla/5.0")).isFalse();
            assertThat(checker.isAllowed(base + "/cats.xls?personality=loki", "Mozilla/5.0")).isTrue();
        }

        @Test
        @DisplayName("UTF-8로 인코딩된 한글 값도 깨지지 않고 파싱된다")
        void UTF8_한글_인코딩() throws Exception {
            String base = startServer(200, """
                    # 한글 주석: 관리자 페이지 차단
                    User-agent: *
                    Disallow: /관리자/
                    """, "text/plain; charset=UTF-8");

            assertThat(checker.isAllowed(base + "/관리자/설정", "Mozilla/5.0")).isFalse();
            assertThat(checker.isAllowed(base + "/공개", "Mozilla/5.0")).isTrue();
        }
    }
}
