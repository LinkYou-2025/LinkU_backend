package com.umc.linkyou.infra.parser;

import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.CrawlStrategy;
import com.umc.linkyou.infra.net.SafeUrlFetcher;
import com.umc.linkyou.infra.net.SsrfGuard;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebContentExtractor 테스트")
class WebContentExtractorTest {

    @InjectMocks
    private WebContentExtractor webContentExtractor;

    @Mock private DomainRepository domainRepository;
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
            @DisplayName("robots.txt가 허용하고 등록되지 않은 도메인이면 기본 전략으로 본문을 추출한다")
            void robots_허용시_기본전략으로_본문을_추출한다() throws Exception {
                // given
                given(robotsTxtChecker.isAllowed(URL, UA)).willReturn(true);
                given(domainRepository.findByDomainTail("example.com")).willReturn(Optional.empty());
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
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> webContentExtractor.extractTextFromUrl(URL));

                assertEquals(AiArticleErrorStatus._CONTENT_EXTRACTION_PROHIBITED, ex.getCode());
                verifyNoInteractions(safeUrlFetcher);
            }

            @Test
            @DisplayName("본문 추출 결과가 비어 있으면 추출 실패 예외를 던진다")
            void 본문이_비어있으면_예외를_던진다() throws Exception {
                // given
                given(robotsTxtChecker.isAllowed(URL, UA)).willReturn(true);
                given(domainRepository.findByDomainTail("example.com")).willReturn(Optional.empty());
                Document emptyDoc = Jsoup.parse("<html><body></body></html>");
                given(safeUrlFetcher.fetchDocument(eq(URL), eq(UA), anyInt())).willReturn(emptyDoc);

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> webContentExtractor.extractTextFromUrl(URL));

                assertEquals(AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED, ex.getCode());
            }

            @Test
            @DisplayName("SSRF 정책에 의해 차단되면 크롤링 금지 예외를 던진다")
            void SSRF_차단시_예외를_던진다() throws Exception {
                // given
                // domainRepository.findByDomainTail 은 extractTextFromDocument()에서만 호출되는데,
                // fetchDocument 단계에서 바로 예외가 나므로 여기까지 도달하지 않는다 (스텁하면 UnnecessaryStubbingException).
                given(robotsTxtChecker.isAllowed(URL, UA)).willReturn(true);
                given(safeUrlFetcher.fetchDocument(eq(URL), eq(UA), anyInt()))
                        .willThrow(new SsrfGuard.BlockedException("사설 IP 차단"));

                // when & then
                GeneralException ex = assertThrows(GeneralException.class,
                        () -> webContentExtractor.extractTextFromUrl(URL));

                assertEquals(AiArticleErrorStatus._CONTENT_EXTRACTION_PROHIBITED, ex.getCode());
            }
        }
    }

    @Nested
    @DisplayName("네이버 블로그(IFRAME) 전략")
    class NaverBlogStrategy {

        private static final String NAVER_URL = "https://blog.naver.com/someuser/12345";
        private static final String IFRAME_URL = "https://blog.naver.com/PostView.naver?blogId=someuser&logNo=12345";

        private void 네이버_도메인으로_등록되어_있다() {
            Domain naverDomain = Domain.builder()
                    .name("naver")
                    .domainTail("blog.naver.com")
                    .crawlStrategy(CrawlStrategy.IFRAME)
                    .build();
            given(domainRepository.findByDomainTail("blog.naver.com")).willReturn(Optional.of(naverDomain));
        }

        @Test
        @DisplayName("iframe URL이 robots.txt에 의해 막히면 원본 페이지 본문으로 폴백한다")
        void iframe이_robots에_막히면_원본_본문으로_폴백한다() throws Exception {
            // given
            given(robotsTxtChecker.isAllowed(NAVER_URL, UA)).willReturn(true);
            네이버_도메인으로_등록되어_있다();

            Document outerDoc = Jsoup.parse(
                    "<html><body>바깥 래퍼 페이지"
                            + "<iframe id=\"mainFrame\" src=\"/PostView.naver?blogId=someuser&logNo=12345\"></iframe>"
                            + "</body></html>");
            given(safeUrlFetcher.fetchDocument(eq(NAVER_URL), eq(UA), anyInt())).willReturn(outerDoc);
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
            네이버_도메인으로_등록되어_있다();

            Document outerDoc = Jsoup.parse(
                    "<html><body><iframe id=\"mainFrame\" src=\"/PostView.naver?blogId=someuser&logNo=12345\"></iframe></body></html>");
            given(safeUrlFetcher.fetchDocument(eq(NAVER_URL), eq(UA), anyInt())).willReturn(outerDoc);
            given(robotsTxtChecker.isAllowed(IFRAME_URL, UA)).willReturn(true);

            Document iframeDoc = Jsoup.parse(
                    "<html><body><div id=\"post-view12345\"><div class=\"se-main-container\">진짜 본문 내용</div></div></body></html>");
            given(safeUrlFetcher.fetchDocument(eq(IFRAME_URL), eq(UA), anyInt())).willReturn(iframeDoc);

            // when
            String result = webContentExtractor.extractTextFromUrl(NAVER_URL);

            // then
            assertEquals("진짜 본문 내용", result);
        }
    }

    @Nested
    @DisplayName("도메인 tail 계층 매칭 폴백 (서브도메인 → apex)")
    class DomainTailHierarchyFallback {

        private static final String TISTORY_URL = "https://someuser.tistory.com/123";

        @Test
        @DisplayName("정확히 일치하는 도메인이 없으면 registry-suffix apex(tistory.com) 도메인의 전략으로 폴백한다")
        void 정확히_일치하지_않으면_apex_전략으로_폴백한다() throws Exception {
            // given
            given(robotsTxtChecker.isAllowed(TISTORY_URL, UA)).willReturn(true);
            given(domainRepository.findByDomainTail("someuser.tistory.com")).willReturn(Optional.empty());
            Domain tistoryApex = Domain.builder()
                    .name("tistory")
                    .domainTail("tistory.com")
                    .crawlStrategy(CrawlStrategy.BODY)
                    .build();
            given(domainRepository.findByDomainTail("tistory.com")).willReturn(Optional.of(tistoryApex));

            // DefaultExtractor라면 <article> 태그만 뽑아 "본문만"을 반환하지만,
            // apex 도메인의 BODY 전략이 제대로 선택됐다면 body 전체 텍스트가 반환되어야 한다.
            Document doc = Jsoup.parse(
                    "<html><body><article>본문만</article><footer>푸터도 포함</footer></body></html>");
            given(safeUrlFetcher.fetchDocument(eq(TISTORY_URL), eq(UA), anyInt())).willReturn(doc);

            // when
            String result = webContentExtractor.extractTextFromUrl(TISTORY_URL);

            // then
            assertEquals("본문만 푸터도 포함", result);
        }

        @Test
        @DisplayName("정확한 호스트도, apex 도메인도 등록되어 있지 않으면 기본 전략(DefaultExtractor)으로 폴백한다")
        void 아무_후보도_매칭되지_않으면_기본_전략을_사용한다() throws Exception {
            // given
            given(robotsTxtChecker.isAllowed(TISTORY_URL, UA)).willReturn(true);
            given(domainRepository.findByDomainTail("someuser.tistory.com")).willReturn(Optional.empty());
            given(domainRepository.findByDomainTail("tistory.com")).willReturn(Optional.empty());

            Document doc = Jsoup.parse(
                    "<html><body><article>기본 전략은 article을 우선한다</article><footer>푸터</footer></body></html>");
            given(safeUrlFetcher.fetchDocument(eq(TISTORY_URL), eq(UA), anyInt())).willReturn(doc);

            // when
            String result = webContentExtractor.extractTextFromUrl(TISTORY_URL);

            // then
            assertEquals("기본 전략은 article을 우선한다", result);
        }
    }
}
