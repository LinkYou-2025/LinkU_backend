package com.umc.linkyou.infra.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.net.SsrfGuard;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebContentExtractor 테스트")
class WebContentExtractorTest {

    @InjectMocks private WebContentExtractor webContentExtractor;

    @Mock private RobotsTxtChecker robotsTxtChecker;
    @Mock private SafeUrlFetcher safeUrlFetcher;

    private static final String URL = "https://example.com/article";
    private static final String UA = "Mozilla/5.0";

    @Nested
    @DisplayName("extractTextFromUrl")
    class ExtractTextFromUrl {

        @Nested
        @DisplayName("성공")
        class Success {

            @Test
            @DisplayName("robots.txt가 허용하고 전용 추출기가 없는 사이트면 기본 추출기로 본문을 추출한다")
            void robots_허용시_기본전략으로_본문을_추출한다() throws Exception {
                // given
                given(robotsTxtChecker.isAllowed(URL, UA)).willReturn(true);
                Document doc = Jsoup.parse("<html><body><article>본문 내용입니다</article></body></html>");
                given(safeUrlFetcher.fetchDocument(eq(URL), eq(UA), anyInt())).willReturn(doc);

                // when
                String result = webContentExtractor.extractTextFromUrl(URL);

                // then
                assertEquals("본문 내용입니다", result);
            }
        }

        @Nested
        @DisplayName("실패")
        class Failure {

            @Test
            @DisplayName("robots.txt가 불허하면 크롤링 금지 예외를 던지고 fetch는 시도하지 않는다")
            void robots_불허시_예외를_던진다() {
                // given
                given(robotsTxtChecker.isAllowed(URL, UA)).willReturn(false);

                // when & then
                GeneralException ex =
                        assertThrows(
                                GeneralException.class,
                                () -> webContentExtractor.extractTextFromUrl(URL));

                assertEquals(AiArticleErrorStatus._CONTENT_EXTRACTION_PROHIBITED, ex.getCode());
                verifyNoInteractions(safeUrlFetcher);
            }

            @Test
            @DisplayName("본문 추출 결과가 비어 있으면 추출 실패 예외를 던진다")
            void 본문이_비어있으면_예외를_던진다() throws Exception {
                // given
                given(robotsTxtChecker.isAllowed(URL, UA)).willReturn(true);
                Document emptyDoc = Jsoup.parse("<html><body></body></html>");
                given(safeUrlFetcher.fetchDocument(eq(URL), eq(UA), anyInt())).willReturn(emptyDoc);

                // when & then
                GeneralException ex =
                        assertThrows(
                                GeneralException.class,
                                () -> webContentExtractor.extractTextFromUrl(URL));

                assertEquals(AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED, ex.getCode());
            }

            @Test
            @DisplayName("SSRF 정책에 의해 차단되면 크롤링 금지 예외를 던진다")
            void SSRF_차단시_예외를_던진다() throws Exception {
                // given
                given(robotsTxtChecker.isAllowed(URL, UA)).willReturn(true);
                given(safeUrlFetcher.fetchDocument(eq(URL), eq(UA), anyInt()))
                        .willThrow(new SsrfGuard.BlockedException("사설 IP 차단"));

                // when & then
                GeneralException ex =
                        assertThrows(
                                GeneralException.class,
                                () -> webContentExtractor.extractTextFromUrl(URL));

                assertEquals(AiArticleErrorStatus._CONTENT_EXTRACTION_PROHIBITED, ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("네이버 블로그 추출기")
    class NaverBlogStrategy {

        private static final String NAVER_URL = "https://blog.naver.com/someuser/12345";
        private static final String IFRAME_URL =
                "https://blog.naver.com/PostView.naver?blogId=someuser&logNo=12345";

        @Test
        @DisplayName("iframe URL이 robots.txt에 의해 막히면 원본 페이지 본문으로 폴백한다")
        void iframe이_robots에_막히면_원본_본문으로_폴백한다() throws Exception {
            // given
            given(robotsTxtChecker.isAllowed(NAVER_URL, UA)).willReturn(true);

            Document outerDoc =
                    Jsoup.parse(
                            "<html><body>바깥 래퍼 페이지"
                                    + "<iframe id=\"mainFrame\" src=\"/PostView.naver?blogId=someuser&logNo=12345\"></iframe>"
                                    + "</body></html>");
            given(safeUrlFetcher.fetchDocument(eq(NAVER_URL), eq(UA), anyInt()))
                    .willReturn(outerDoc);
            given(robotsTxtChecker.isAllowed(IFRAME_URL, UA)).willReturn(false);

            // when
            String result = webContentExtractor.extractTextFromUrl(NAVER_URL);

            // then
            assertEquals("바깥 래퍼 페이지", result);
            verify(safeUrlFetcher, never()).fetchDocument(eq(IFRAME_URL), any(), anyInt());
        }

        @Test
        @DisplayName("iframe URL이 허용되면 iframe 본문을 추출한다")
        void iframe이_허용되면_iframe_본문을_추출한다() throws Exception {
            // given
            given(robotsTxtChecker.isAllowed(NAVER_URL, UA)).willReturn(true);

            Document outerDoc =
                    Jsoup.parse(
                            "<html><body><iframe id=\"mainFrame\" src=\"/PostView.naver?blogId=someuser&logNo=12345\"></iframe></body></html>");
            given(safeUrlFetcher.fetchDocument(eq(NAVER_URL), eq(UA), anyInt()))
                    .willReturn(outerDoc);
            given(robotsTxtChecker.isAllowed(IFRAME_URL, UA)).willReturn(true);

            Document iframeDoc =
                    Jsoup.parse(
                            "<html><body><div id=\"post-view12345\"><div class=\"se-main-container\">진짜 본문 내용</div></div></body></html>");
            given(safeUrlFetcher.fetchDocument(eq(IFRAME_URL), eq(UA), anyInt()))
                    .willReturn(iframeDoc);

            // when
            String result = webContentExtractor.extractTextFromUrl(NAVER_URL);

            // then
            assertEquals("진짜 본문 내용", result);
        }

        @Test
        @DisplayName("이미 iframe 안 문서를 받으면 다시 요청하지 않고 본문을 추출한다")
        void 이미_iframe_안_문서를_받으면_다시_요청하지_않고_본문을_추출한다() throws Exception {
            // given
            Document iframeDoc =
                    Jsoup.parse(
                            "<html><body><div id=\"post-view12345\"><div class=\"se-main-container\">진짜 본문 내용</div></div></body></html>");

            // when
            String result = webContentExtractor.extractTextFromDocument(IFRAME_URL, iframeDoc);

            // then
            assertEquals("진짜 본문 내용", result);
            verify(safeUrlFetcher, never()).fetchDocument(any(), any(), anyInt());
        }
    }

    @Nested
    @DisplayName("사이트별 추출기 선택")
    class StrategySelection {

        @Test
        @DisplayName("모바일 네이버 블로그는 iframe 없이 바로 글 본문을 추출한다")
        void 모바일_네이버_블로그는_iframe_없이_바로_글_본문을_추출한다() {
            Document doc =
                    Jsoup.parse(
                            "<html><body><nav>메뉴</nav>"
                                    + "<div class=\"se-main-container\">모바일 본문</div></body></html>");

            String result =
                    webContentExtractor.extractTextFromDocument(
                            "https://m.blog.naver.com/someuser/12345", doc);

            assertEquals("모바일 본문", result);
            verifyNoInteractions(safeUrlFetcher);
        }

        @Test
        @DisplayName("네이버 블로그가 아닌 네이버 서비스는 기본 추출기를 쓴다")
        void 네이버_블로그가_아닌_네이버_서비스는_기본_추출기를_쓴다() {
            Document doc =
                    Jsoup.parse(
                            "<html><body><nav>메뉴</nav><article>기사 본문</article><footer>푸터</footer></body></html>");

            String result =
                    webContentExtractor.extractTextFromDocument(
                            "https://n.news.naver.com/article/008/1", doc);

            assertEquals("기사 본문", result);
        }

        @Test
        @DisplayName("다른 사이트에 mainFrame iframe이 있어도 따라가지 않고 기본 추출기를 쓴다")
        void 다른_사이트에_mainFrame_iframe이_있어도_따라가지_않고_기본_추출기를_쓴다() {
            Document doc =
                    Jsoup.parse(
                            "<html><body><article>본문</article>"
                                    + "<iframe id=\"mainFrame\" src=\"https://ads.example.net/widget\"></iframe></body></html>");

            String result =
                    webContentExtractor.extractTextFromDocument(
                            "https://someuser.tistory.com/123", doc);

            assertEquals("본문", result);
            verifyNoInteractions(safeUrlFetcher);
        }
    }
}
