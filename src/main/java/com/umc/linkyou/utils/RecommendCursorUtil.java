package com.umc.linkyou.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 홈화면 링크 추천 커서 페이징 인코더/디코더. novelty/normal 두 버킷의 seek 탐색 키를
 * base64(JSON)로 묶어 FE에는 불투명 문자열로만 내려준다 (docs/home-recommend-cursor-api-spec.md 참고).
 */
@Slf4j
public class RecommendCursorUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RecommendCursorUtil() {}

    /** *Bucket/*LastId가 null이면 해당 버킷 첫 페이지(seek 쿼리가 "처음부터"로 해석) */
    public record RecommendCursor(
            Integer noveltyBucket, Long noveltyLastId,
            Integer normalBucket, Long normalLastId,
            boolean noveltyExhausted) {
        public static final RecommendCursor FIRST_PAGE = new RecommendCursor(null, null, null, null, false);
    }

    /** 디코딩 실패 시 에러 대신 첫 페이지로 폴백(로그 남김) — cursor는 외부(FE) 입력이라 조용히 삼키지 않는다 */
    public static RecommendCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return RecommendCursor.FIRST_PAGE;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(Base64.getDecoder().decode(cursor));
            return new RecommendCursor(
                    readNullableInt(node, "noveltyBucket"),
                    readNullableLong(node, "noveltyLastId"),
                    readNullableInt(node, "normalBucket"),
                    readNullableLong(node, "normalLastId"),
                    node.path("noveltyExhausted").asBoolean(false)
            );
        } catch (Exception e) {
            log.warn("[추천 커서 디코딩 실패] 첫 페이지로 폴백합니다. cursor={}", cursor, e);
            return RecommendCursor.FIRST_PAGE;
        }
    }

    private static Integer readNullableInt(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asInt();
    }

    private static Long readNullableLong(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asLong();
    }

    /** 필드가 고정된 int/long/boolean뿐이라 실패할 일이 없어 try-catch 없이 직접 조립 */
    public static String encode(RecommendCursor cursor) {
        String json = "{\"noveltyBucket\":%s,\"noveltyLastId\":%s,\"normalBucket\":%s,\"normalLastId\":%s,\"noveltyExhausted\":%b}"
                .formatted(
                        jsonNumber(cursor.noveltyBucket()),
                        jsonNumber(cursor.noveltyLastId()),
                        jsonNumber(cursor.normalBucket()),
                        jsonNumber(cursor.normalLastId()),
                        cursor.noveltyExhausted());
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String jsonNumber(Number value) {
        return value == null ? "null" : value.toString();
    }
}
