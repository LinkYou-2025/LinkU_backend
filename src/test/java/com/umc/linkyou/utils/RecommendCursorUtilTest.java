package com.umc.linkyou.utils;

import static com.umc.linkyou.support.fixture.HomeRecommendFixture.encodedJson;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class RecommendCursorUtilTest {
    @Nested
    @DisplayName("성공")
    class Success {
        @Test
        @DisplayName("정상_커서_인코딩_후_디코딩_시_점수와_ID를_복원한다")
        void 정상_커서_인코딩_후_디코딩_시_점수와_ID를_복원한다() {
            var cursor = new RecommendCursorUtil.RecommendCursor(150, Long.MAX_VALUE);
            assertEquals(cursor, RecommendCursorUtil.decode(RecommendCursorUtil.encode(cursor)));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        @DisplayName("커서가_비어있을_시_첫_페이지를_반환한다")
        void 커서가_비어있을_시_첫_페이지를_반환한다(String cursor) {
            assertEquals(RecommendCursorUtil.RecommendCursor.FIRST_PAGE, RecommendCursorUtil.decode(cursor));
        }

        @Test
        @DisplayName("점수가_0인_커서_입력_시_유효한_커서로_처리한다")
        void 점수가_0인_커서_입력_시_유효한_커서로_처리한다() {
            var cursor = new RecommendCursorUtil.RecommendCursor(0, 1L);
            assertEquals(cursor, RecommendCursorUtil.decode(RecommendCursorUtil.encode(cursor)));
        }
    }

    @Nested
    @DisplayName("실패")
    class Failure {
        @Test
        @DisplayName("Base64가_잘못된_입력_시_첫_페이지를_반환한다")
        void Base64가_잘못된_입력_시_첫_페이지를_반환한다() {
            assertEquals(RecommendCursorUtil.RecommendCursor.FIRST_PAGE, RecommendCursorUtil.decode("%%%"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "not-json", "null", "[]", "{}",
                "{\"normalBucket\":100,\"normalLastId\":7,\"noveltyExhausted\":false}",
                "{\"scoreBucket\":100}", "{\"lastId\":7}",
                "{\"scoreBucket\":null,\"lastId\":7}",
                "{\"scoreBucket\":100,\"lastId\":null}",
                "{\"scoreBucket\":\"abc\",\"lastId\":7}",
                "{\"scoreBucket\":100,\"lastId\":\"abc\"}",
                "{\"scoreBucket\":2147483648,\"lastId\":7}",
                "{\"scoreBucket\":100,\"lastId\":9223372036854775808}"
        })
        @DisplayName("구버전이나_손상된_커서_입력_시_첫_페이지를_반환한다")
        void 구버전이나_손상된_커서_입력_시_첫_페이지를_반환한다(String json) {
            assertEquals(RecommendCursorUtil.RecommendCursor.FIRST_PAGE,
                    RecommendCursorUtil.decode(encodedJson(json)));
        }
    }
}
