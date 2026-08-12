package com.umc.linkyou.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 홈화면 링크 추천 커서 페이징 인코더/디코더. (scoreBucket, userLinkuId) seek 탐색 키를
 * base64(JSON, {@code {"scoreBucket":<int|null>,"lastId":<long|null>}})로 묶어 FE에는 불투명 문자열로만
 * 내려준다 — FE는 이 문자열을 파싱하지 않고 다음 요청의 cursor 파라미터에 그대로 되돌려주기만 한다.
 */
@Slf4j
public class RecommendCursorUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RecommendCursorUtil() {}

    /** scoreBucket/lastId가 null이면 첫 페이지(seek 쿼리가 "처음부터"로 해석) */
    public record RecommendCursor(Integer scoreBucket, Long lastId) {
        public static final RecommendCursor FIRST_PAGE = new RecommendCursor(null, null);
    }

    /** 디코딩 실패 시 에러 대신 첫 페이지로 폴백(로그 남김) — cursor는 외부(FE) 입력이라 조용히 삼키지 않는다 */
    public static RecommendCursor decode(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return RecommendCursor.FIRST_PAGE;
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(Base64.getDecoder().decode(cursor));
            Integer scoreBucket = readNullableInt(node, "scoreBucket");
            Long lastId = readNullableLong(node, "lastId");
            // JSON 파싱 자체는 성공했지만 scoreBucket/lastId가 둘 다 없는 경우 — 예전 커서 포맷
            // (noveltyBucket/normalBucket 등, 필드 구조가 바뀌기 전)이거나 알 수 없는 형식이다.
            // encode()가 만드는 정상 커서는 이 케이스가 나올 수 없으므로(hasNext=true일 때만 인코딩하고
            // 그때는 항상 마지막 행의 scoreBucket/lastId가 채워짐), 디코딩 실패와 동일하게 취급해
            // 조용히 삼키지 않고 첫 페이지로 폴백한다.
            if (scoreBucket == null && lastId == null) {
                log.warn("[추천 커서 디코딩 실패] 알 수 없는 커서 형식(예: 구버전 포맷)이라 첫 페이지로 폴백합니다. cursor={}", cursor);
                return RecommendCursor.FIRST_PAGE;
            }
            return new RecommendCursor(scoreBucket, lastId);
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

    /** 필드가 고정된 int/long뿐이라 실패할 일이 없어 try-catch 없이 직접 조립 */
    public static String encode(RecommendCursor cursor) {
        String json = "{\"scoreBucket\":%s,\"lastId\":%s}"
                .formatted(jsonNumber(cursor.scoreBucket()), jsonNumber(cursor.lastId()));
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String jsonNumber(Number value) {
        return value == null ? "null" : value.toString();
    }
}
