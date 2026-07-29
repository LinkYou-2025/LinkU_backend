package com.umc.linkyou.infra.parser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// isImageUrl은 private이라 ReflectionTestUtils로 직접 호출한다.
// searchFirstDirectImageUrl(공개 메서드)을 통해 테스트하면 진짜 RestClient로
// 외부 API(Google Custom Search)를 호출하는 코드 경로까지 타버려서, 확장자 판별이라는
// 관심사와 무관하게 테스트가 네트워크에 의존하게 된다.
@DisplayName("CustomSearchImageClient 테스트")
class CustomSearchImageClientTest {

    private final CustomSearchImageClient customSearchImageClient = new CustomSearchImageClient();

    @Nested
    @DisplayName("isImageUrl")
    class IsImageUrl {

        @Test
        @DisplayName("쿼리스트링 없이 이미지 확장자로 끝나면 true를 반환한다")
        void 확장자만_있으면_true() {
            boolean result = ReflectionTestUtils.invokeMethod(
                    customSearchImageClient, "isImageUrl", "https://example.com/photo.jpg");

            assertTrue(result);
        }

        @Test
        @DisplayName("이미지 확장자 뒤에 쿼리스트링이 붙어도 true를 반환한다")
        void 쿼리스트링_붙어도_true() {
            boolean result = ReflectionTestUtils.invokeMethod(
                    customSearchImageClient, "isImageUrl", "https://cdn.example.com/photo.jpg?w=800&auto=format");

            assertTrue(result);
        }

        @Test
        @DisplayName("대문자 확장자여도 true를 반환한다")
        void 대문자_확장자_true() {
            boolean result = ReflectionTestUtils.invokeMethod(
                    customSearchImageClient, "isImageUrl", "https://example.com/PHOTO.PNG?token=abc");

            assertTrue(result);
        }

        @Test
        @DisplayName("이미지 확장자가 아니면 false를 반환한다")
        void 이미지_확장자_아니면_false() {
            boolean result = ReflectionTestUtils.invokeMethod(
                    customSearchImageClient, "isImageUrl", "https://example.com/article.html");

            assertFalse(result);
        }

        @Test
        @DisplayName("확장자 없이 쿼리스트링만 있으면 false를 반환한다")
        void 확장자_없이_쿼리스트링만_false() {
            boolean result = ReflectionTestUtils.invokeMethod(
                    customSearchImageClient, "isImageUrl", "https://cdn.example.com/image?w=800");

            assertFalse(result);
        }
    }
}
