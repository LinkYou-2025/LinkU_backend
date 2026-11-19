package com.umc.linkyou.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.net.SafeUrlFetcher.RedirectHop;

@DisplayName("CanonicalUrlPolicy 테스트")
class CanonicalUrlPolicyTest {

    private static final String ARTICLE = "https://blog.example.com/post/123";

    @Nested
    @DisplayName("① 리다이렉트 판단")
    class FollowTrustedRedirects {

        @Test
        @DisplayName("리다이렉트가 없으면 입력 URL을 그대로 쓴다")
        void 리다이렉트가_없으면_입력_URL을_그대로_쓴다() {
            assertEquals(ARTICLE, CanonicalUrlPolicy.followTrustedRedirects(ARTICLE, List.of()));
        }

        @Test
        @DisplayName("301 영구 이동 시 목적지를 쓴다")
        void 영구_이동_시_목적지를_쓴다() {
            String input = "http://blog.example.com/post/123";
            List<RedirectHop> hops = List.of(new RedirectHop(input, 301, ARTICLE));

            assertEquals(ARTICLE, CanonicalUrlPolicy.followTrustedRedirects(input, hops));
        }

        @Test
        @DisplayName("다른 사이트로 가는 302 임시 이동 시 목적지를 쓴다")
        void 다른_사이트로_가는_302_임시_이동_시_목적지를_쓴다() {
            String input = "https://bit.ly/abc";
            List<RedirectHop> hops = List.of(new RedirectHop(input, 302, ARTICLE));

            assertEquals(ARTICLE, CanonicalUrlPolicy.followTrustedRedirects(input, hops));
        }

        @Test
        @DisplayName("같은 사이트 안의 302 임시 이동 시 입력 URL을 유지한다")
        void 같은_사이트_안의_302_임시_이동_시_입력_URL을_유지한다() {
            List<RedirectHop> hops =
                    List.of(new RedirectHop(ARTICLE, 302, "https://login.example.com/signin"));

            assertEquals(ARTICLE, CanonicalUrlPolicy.followTrustedRedirects(ARTICLE, hops));
        }

        @Test
        @DisplayName("다른 사이트로 가는 meta refresh 시 목적지를 쓴다")
        void 다른_사이트로_가는_meta_refresh_시_목적지를_쓴다() {
            String input = "https://l.sns.com/r?u=abc";
            List<RedirectHop> hops =
                    List.of(new RedirectHop(input, SafeUrlFetcher.META_REFRESH_STATUS, ARTICLE));

            assertEquals(ARTICLE, CanonicalUrlPolicy.followTrustedRedirects(input, hops));
        }

        @Test
        @DisplayName("단축 URL 다음 같은 사이트 302를 만나면 그 앞까지만 따라간다")
        void 단축_URL_다음_같은_사이트_302를_만나면_그_앞까지만_따라간다() {
            String input = "https://bit.ly/abc";
            List<RedirectHop> hops =
                    List.of(
                            new RedirectHop(input, 302, ARTICLE),
                            new RedirectHop(
                                    ARTICLE,
                                    302,
                                    "https://blog.example.com/login?next=%2Fpost%2F123"));

            assertEquals(ARTICLE, CanonicalUrlPolicy.followTrustedRedirects(input, hops));
        }

        @Test
        @DisplayName("경로가 있던 URL이 루트로 이동 시 입력 URL을 유지한다")
        void 경로가_있던_URL이_루트로_이동_시_입력_URL을_유지한다() {
            List<RedirectHop> hops =
                    List.of(new RedirectHop(ARTICLE, 301, "https://blog.example.com/"));

            assertEquals(ARTICLE, CanonicalUrlPolicy.followTrustedRedirects(ARTICLE, hops));
        }
    }

    @Nested
    @DisplayName("② 페이지가 밝힌 대표 주소")
    class DeclaredUrl {

        @Test
        @DisplayName("보이는 로그인 폼의 password autocomplete는 로그인 화면으로 판별한다")
        void 보이는_로그인_폼의_password_autocomplete는_로그인_화면으로_판별한다() {
            Document doc =
                    html(
                            ARTICLE,
                            "<form><input type=\"password\" autocomplete=\"current-password\"></form>");

            assertTrue(CanonicalUrlPolicy.hasPasswordLoginForm(doc));
        }

        @Test
        @DisplayName("사용자명 입력과 함께 있는 비밀번호 입력은 로그인 화면으로 판별한다")
        void 사용자명_입력과_함께_있는_비밀번호_입력은_로그인_화면으로_판별한다() {
            Document doc =
                    html(
                            ARTICLE,
                            "<form><input autocomplete=\"username\"><input type=\"password\"></form>");

            assertTrue(CanonicalUrlPolicy.hasPasswordLoginForm(doc));
        }

        @Test
        @DisplayName("숨겨진 로그인 템플릿은 로그인 화면으로 판별하지 않는다")
        void 숨겨진_로그인_템플릿은_로그인_화면으로_판별하지_않는다() {
            Document doc =
                    html(
                            ARTICLE,
                            "<form hidden><input autocomplete=\"username\"><input type=\"password\"></form>");

            assertEquals(false, CanonicalUrlPolicy.hasPasswordLoginForm(doc));
        }

        @Test
        @DisplayName("폼 밖에 있는 비밀번호 입력은 로그인 화면으로 판별하지 않는다")
        void 폼_밖에_있는_비밀번호_입력은_로그인_화면으로_판별하지_않는다() {
            Document doc =
                    html(ARTICLE, "<input type=\"password\" autocomplete=\"current-password\">");

            assertEquals(false, CanonicalUrlPolicy.hasPasswordLoginForm(doc));
        }

        @Test
        @DisplayName("canonical과 og:url이 다르면 canonical을 쓴다")
        void canonical과_og_url이_다르면_canonical을_쓴다() {
            // 티스토리 모바일: canonical은 PC 주소, og:url은 모바일 주소
            String landing = "https://notice.tistory.com/m/2705";
            Document doc =
                    html(
                            landing,
                            "<link rel=\"canonical\" href=\"https://notice.tistory.com/2705\">"
                                    + "<meta property=\"og:url\" content=\"https://notice.tistory.com/m/2705\">");

            assertEquals(
                    Optional.of("https://notice.tistory.com/2705"),
                    CanonicalUrlPolicy.declaredUrl(doc, landing));
        }

        @Test
        @DisplayName("canonical이 없으면 og:url을 쓴다")
        void canonical이_없으면_og_url을_쓴다() {
            // 네이버 블로그 모바일: og:url만 있고 PC 주소를 가리킨다
            String landing = "https://m.blog.naver.com/user/224456322072";
            Document doc =
                    html(
                            landing,
                            "<meta property=\"og:url\" content=\"https://blog.naver.com/user/224456322072\">");

            assertEquals(
                    Optional.of("https://blog.naver.com/user/224456322072"),
                    CanonicalUrlPolicy.declaredUrl(doc, landing));
        }

        @Test
        @DisplayName("canonical이 쿼리를 뺀 주소면 추적 파라미터가 사라진다")
        void canonical이_쿼리를_뺀_주소면_추적_파라미터가_사라진다() {
            String landing = "https://namu.wiki/w/abc?from=share";
            Document doc =
                    html(landing, "<link rel=\"canonical\" href=\"https://namu.wiki/w/abc\">");

            assertEquals(
                    Optional.of("https://namu.wiki/w/abc"),
                    CanonicalUrlPolicy.declaredUrl(doc, landing));
        }

        @Test
        @DisplayName("상대 경로 canonical은 도착 페이지 기준으로 절대 주소가 된다")
        void 상대_경로_canonical은_도착_페이지_기준으로_절대_주소가_된다() {
            String landing = "https://blog.example.com/post/123?ref=x";
            Document doc = html(landing, "<link rel=\"canonical\" href=\"/post/123\">");

            assertEquals(Optional.of(ARTICLE), CanonicalUrlPolicy.declaredUrl(doc, landing));
        }

        @Test
        @DisplayName("같은 플랫폼의 다른 호스트를 가리키면 쓰지 않는다")
        void 같은_플랫폼의_다른_호스트를_가리키면_쓰지_않는다() {
            String landing = "https://a.tistory.com/1";
            Document doc =
                    html(landing, "<link rel=\"canonical\" href=\"https://b.tistory.com/1\">");

            assertEquals(Optional.empty(), CanonicalUrlPolicy.declaredUrl(doc, landing));
        }

        @Test
        @DisplayName("홈 주소를 가리키면 쓰지 않는다")
        void 홈_주소를_가리키면_쓰지_않는다() {
            Document doc =
                    html(ARTICLE, "<link rel=\"canonical\" href=\"https://blog.example.com/\">");

            assertEquals(Optional.empty(), CanonicalUrlPolicy.declaredUrl(doc, ARTICLE));
        }

        @Test
        @DisplayName("canonical이 잘못됐으면 og:url로 넘어간다")
        void canonical이_잘못됐으면_og_url로_넘어간다() {
            Document doc =
                    html(
                            ARTICLE,
                            "<link rel=\"canonical\" href=\"https://blog.example.com/\">"
                                    + "<meta property=\"og:url\" content=\""
                                    + ARTICLE
                                    + "\">");

            assertEquals(Optional.of(ARTICLE), CanonicalUrlPolicy.declaredUrl(doc, ARTICLE));
        }
    }

    @Nested
    @DisplayName("사이트 규칙")
    class SiteRules {

        @Test
        @DisplayName("네이버 뉴스 mnews 주소는 article 주소로 맞춘다")
        void 네이버_뉴스_mnews_주소는_article_주소로_맞춘다() {
            assertEquals(
                    "https://n.news.naver.com/article/008/0005435560",
                    CanonicalUrlPolicy.applySiteRules(
                            "https://n.news.naver.com/mnews/article/008/0005435560"));
        }

        @Test
        @DisplayName("규칙 대상이 아니면 그대로 둔다")
        void 규칙_대상이_아니면_그대로_둔다() {
            assertEquals(ARTICLE, CanonicalUrlPolicy.applySiteRules(ARTICLE));
        }
    }

    private static Document html(String baseUri, String head) {
        return Jsoup.parse("<html><head>" + head + "</head><body>본문</body></html>", baseUri);
    }
}
