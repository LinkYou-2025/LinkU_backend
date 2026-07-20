package com.umc.linkyou.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("UrlValidUtils 테스트")
class UrlValidUtilsTest {

    @Nested
    @DisplayName("extractDomainTailCandidates")
    class ExtractDomainTailCandidates {

        @Test
        @DisplayName("서브도메인이 없는 apex 도메인은 후보가 자기 자신 하나뿐이다")
        void apex_도메인은_후보가_하나뿐이다() {
            List<String> candidates = UrlValidUtils.extractDomainTailCandidates("https://example.com/post");

            assertEquals(List.of("example.com"), candidates);
        }

        @Test
        @DisplayName("www. 접두사는 제거된 뒤 후보에 들어간다")
        void www_접두사는_제거된다() {
            List<String> candidates = UrlValidUtils.extractDomainTailCandidates("https://www.example.com/post");

            assertEquals(List.of("example.com"), candidates);
        }

        @Test
        @DisplayName("tistory.com 서브도메인은 정확한 호스트와 apex(tistory.com)를 순서대로 후보로 준다")
        void 티스토리_서브도메인은_apex를_2순위_후보로_준다() {
            List<String> candidates =
                    UrlValidUtils.extractDomainTailCandidates("https://someuser.tistory.com/123");

            assertEquals(List.of("someuser.tistory.com", "tistory.com"), candidates);
        }

        @Test
        @DisplayName("blogspot.com 서브도메인도 동일하게 apex로 폴백 후보를 준다")
        void 블로그스팟_서브도메인도_apex를_후보로_준다() {
            List<String> candidates =
                    UrlValidUtils.extractDomainTailCandidates("https://pannchoa.blogspot.com/2024/01/post.html");

            assertEquals(List.of("pannchoa.blogspot.com", "blogspot.com"), candidates);
        }

        @Test
        @DisplayName("github.io 서브도메인도 동일하게 apex로 폴백 후보를 준다 (registry suffix가 io인 경우)")
        void 깃허브io_서브도메인도_apex를_후보로_준다() {
            List<String> candidates =
                    UrlValidUtils.extractDomainTailCandidates("https://someuser.github.io/repo/");

            assertEquals(List.of("someuser.github.io", "github.io"), candidates);
        }

        @Test
        @DisplayName("이미 정확히 seed된 서브도메인(blog.naver.com)도 apex(naver.com)를 2순위 후보로 함께 준다")
        void 네이버_블로그_서브도메인도_apex를_후보에_포함한다() {
            List<String> candidates =
                    UrlValidUtils.extractDomainTailCandidates("https://blog.naver.com/someuser/12345");

            assertEquals(List.of("blog.naver.com", "naver.com"), candidates);
        }

        @Test
        @DisplayName("호스트를 파싱할 수 없으면 빈 목록을 반환한다")
        void 호스트가_없으면_빈_목록을_반환한다() {
            List<String> candidates = UrlValidUtils.extractDomainTailCandidates("not-a-valid-url");

            assertTrue(candidates.isEmpty());
        }

        @Test
        @DisplayName("여러 단계의 서브도메인이어도 registry suffix 바로 아래 라벨로 apex를 계산한다")
        void 다단계_서브도메인도_registry_suffix_바로_아래로_apex를_계산한다() {
            List<String> candidates =
                    UrlValidUtils.extractDomainTailCandidates("https://a.b.someuser.tistory.com/1");

            assertEquals(List.of("a.b.someuser.tistory.com", "tistory.com"), candidates);
        }
    }
}
