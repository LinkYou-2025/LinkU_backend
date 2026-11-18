package com.umc.linkyou.infra.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NaverBlog 테스트")
class NaverBlogTest {

    @Nested
    @DisplayName("isBlogUrl")
    class IsBlogUrl {

        @Test
        @DisplayName("PC와 모바일 블로그 주소는 네이버 블로그로 판정한다")
        void PC와_모바일_블로그_주소는_네이버_블로그로_판정한다() {
            assertTrue(NaverBlog.isBlogUrl("https://blog.naver.com/someuser/12345"));
            assertTrue(NaverBlog.isBlogUrl("https://m.blog.naver.com/someuser/12345"));
        }

        @Test
        @DisplayName("다른 네이버 서비스는 네이버 블로그가 아니다")
        void 다른_네이버_서비스는_네이버_블로그가_아니다() {
            assertFalse(NaverBlog.isBlogUrl("https://kin.naver.com/qna/detail.naver?docId=1"));
            assertFalse(NaverBlog.isBlogUrl("https://n.news.naver.com/article/008/1"));
        }
    }

    @Nested
    @DisplayName("postFrameUrl")
    class PostFrameUrl {

        @Test
        @DisplayName("PC 바깥 페이지면 iframe 주소를 절대 주소로 돌려준다")
        void PC_바깥_페이지면_iframe_주소를_절대_주소로_돌려준다() {
            Document doc =
                    Jsoup.parse(
                            "<html><body><iframe id=\"mainFrame\" src=\"/PostView.naver?blogId=u&logNo=1\"></iframe></body></html>",
                            "https://blog.naver.com/u/1");

            assertEquals(
                    Optional.of("https://blog.naver.com/PostView.naver?blogId=u&logNo=1"),
                    NaverBlog.postFrameUrl(doc));
        }

        @Test
        @DisplayName("iframe이 없으면 empty를 돌려준다")
        void iframe이_없으면_empty를_돌려준다() {
            assertEquals(
                    Optional.empty(),
                    NaverBlog.postFrameUrl(Jsoup.parse("<html><body>본문</body></html>")));
        }
    }

    @Nested
    @DisplayName("logNoOf")
    class LogNoOf {

        @Test
        @DisplayName("PostView 쿼리와 블로그 경로 둘 다에서 글 번호를 꺼낸다")
        void PostView_쿼리와_블로그_경로_둘_다에서_글_번호를_꺼낸다() {
            assertEquals(
                    "123",
                    NaverBlog.logNoOf("https://blog.naver.com/PostView.naver?blogId=u&logNo=123"));
            assertEquals("456", NaverBlog.logNoOf("https://m.blog.naver.com/u/456"));
        }

        @Test
        @DisplayName("글 번호가 없으면 null을 돌려준다")
        void 글_번호가_없으면_null을_돌려준다() {
            assertNull(NaverBlog.logNoOf("https://blog.naver.com/u"));
        }
    }
}
