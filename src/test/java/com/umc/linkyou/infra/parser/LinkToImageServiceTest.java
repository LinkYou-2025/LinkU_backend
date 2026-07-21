package com.umc.linkyou.infra.parser;

import com.umc.linkyou.infra.net.SafeUrlFetcher;
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
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

// extractRepresentativeImage/extractFromNaverBlog는 private이라 ReflectionTestUtils로 직접 호출한다.
// getRelatedImageFromUrl(공개 메서드)을 통해 테스트하면 1차 추출이 실패했을 때
// searchFirstDirectImageUrl()이 진짜 RestTemplate으로 외부 API(Google Custom Search)를 호출하는
// 코드 경로까지 타버려서, robots.txt 차단이라는 관심사와 무관하게 테스트가 네트워크에 의존하게 된다.
@ExtendWith(MockitoExtension.class)
@DisplayName("LinkToImageService 테스트")
class LinkToImageServiceTest {

    @InjectMocks
    private LinkToImageService linkToImageService;

    @Mock private DomainRepository domainRepository;
    @Mock private SafeUrlFetcher safeUrlFetcher;
    @Mock private RobotsTxtChecker robotsTxtChecker;

    private static final String UA = "Mozilla/5.0";

    @Nested
    @DisplayName("extractRepresentativeImage (일반 웹페이지)")
    class ExtractRepresentativeImage {

        private static final String URL = "https://example.com/post";

        @Test
        @DisplayName("robots.txt가 허용하면 og:image를 추출한다")
        void robots_허용시_og_image를_추출한다() throws Exception {
            // given
            given(robotsTxtChecker.isAllowed(URL, UA)).willReturn(true);
            Document doc = Jsoup.parse(
                    "<html><head><meta property=\"og:image\" content=\"https://example.com/thumb.jpg\"></head><body></body></html>");
            given(safeUrlFetcher.fetchDocument(eq(URL), eq(UA), anyInt())).willReturn(doc);

            // when
            String result = ReflectionTestUtils.invokeMethod(linkToImageService, "extractRepresentativeImage", URL);

            // then
            assertEquals("https://example.com/thumb.jpg", result);
        }

        @Test
        @DisplayName("robots.txt가 불허하면 null을 반환하고 fetch는 시도하지 않는다")
        void robots_불허시_null을_반환한다() {
            // given
            given(robotsTxtChecker.isAllowed(URL, UA)).willReturn(false);

            // when
            String result = ReflectionTestUtils.invokeMethod(linkToImageService, "extractRepresentativeImage", URL);

            // then
            assertNull(result);
            verifyNoInteractions(safeUrlFetcher);
        }
    }

    @Nested
    @DisplayName("extractFromNaverBlog")
    class ExtractFromNaverBlog {

        private static final String BLOG_URL = "https://blog.naver.com/someuser/12345";
        private static final String IFRAME_URL = "https://blog.naver.com/PostView.naver?blogId=someuser&logNo=12345";

        @Test
        @DisplayName("원본 블로그 URL이 robots.txt에 막히면 null을 반환하고 fetch는 시도하지 않는다")
        void 원본_URL이_robots에_막히면_null을_반환한다() {
            // given
            given(robotsTxtChecker.isAllowed(BLOG_URL, UA)).willReturn(false);

            // when
            String result = ReflectionTestUtils.invokeMethod(linkToImageService, "extractFromNaverBlog", BLOG_URL);

            // then
            assertNull(result);
            verifyNoInteractions(safeUrlFetcher);
        }

        @Test
        @DisplayName("iframe(실제 본문) URL이 robots.txt에 막히면 null을 반환하고 iframe fetch는 시도하지 않는다")
        void iframe_URL이_robots에_막히면_null을_반환한다() throws Exception {
            // given
            given(robotsTxtChecker.isAllowed(BLOG_URL, UA)).willReturn(true);
            Document outerDoc = Jsoup.parse(
                    "<html><body><iframe id=\"mainFrame\" src=\"/PostView.naver?blogId=someuser&logNo=12345\"></iframe></body></html>");
            given(safeUrlFetcher.fetchDocument(eq(BLOG_URL), eq(UA), anyInt())).willReturn(outerDoc);
            given(robotsTxtChecker.isAllowed(IFRAME_URL, UA)).willReturn(false);

            // when
            String result = ReflectionTestUtils.invokeMethod(linkToImageService, "extractFromNaverBlog", BLOG_URL);

            // then
            assertNull(result);
            verify(safeUrlFetcher, never()).fetchDocument(eq(IFRAME_URL), any(), anyInt());
        }

        @Test
        @DisplayName("둘 다 허용되면 iframe 본문에서 대표 이미지를 추출한다")
        void 모두_허용되면_대표이미지를_추출한다() throws Exception {
            // given
            given(robotsTxtChecker.isAllowed(BLOG_URL, UA)).willReturn(true);
            Document outerDoc = Jsoup.parse(
                    "<html><body><iframe id=\"mainFrame\" src=\"/PostView.naver?blogId=someuser&logNo=12345\"></iframe></body></html>");
            given(safeUrlFetcher.fetchDocument(eq(BLOG_URL), eq(UA), anyInt())).willReturn(outerDoc);
            given(robotsTxtChecker.isAllowed(IFRAME_URL, UA)).willReturn(true);
            Document iframeDoc = Jsoup.parse(
                    "<html><head><meta property=\"og:image\" content=\"https://blog.naver.com/real-thumb.jpg\"></head><body></body></html>");
            given(safeUrlFetcher.fetchDocument(eq(IFRAME_URL), eq(UA), anyInt())).willReturn(iframeDoc);

            // when
            String result = ReflectionTestUtils.invokeMethod(linkToImageService, "extractFromNaverBlog", BLOG_URL);

            // then
            assertEquals("https://blog.naver.com/real-thumb.jpg", result);
        }
    }
}
