package com.umc.linkyou.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 홈화면 링크 추천(GET /linku/recommend) 커서 페이징용 인코더/디코더.
 *
 * novelty(최근에 안 본 것)/normal(7축 가중합) 두 버킷은 서로 다른 속도로 소진되기 때문에 단일 offset 하나로는
 * 진행 위치를 표현할 수 없다. 그래서 두 버킷 각각의 seek(keyset) 탐색 지점과 novelty 소진 여부
 * ({@code noveltyBucket}/{@code noveltyLastId}, {@code normalBucket}/{@code normalLastId},
 * {@code noveltyExhausted})를 base64(JSON)로 인코딩해 FE에는 불투명한 문자열로만 내려준다.
 * FE는 이 값을 파싱/가공하지 않고 다음 요청의 cursor 파라미터에 그대로 복사해 넣기만 하면 된다
 * (docs/home-recommend-cursor-api-spec.md 참고).
 *
 * bucket/lastId는 OFFSET이 아니라 UsersLinkuRepositoryImpl의 seek 쿼리가 쓰는 탐색 키다 — 각각
 * "정렬에 쓰인 score를 양자화한 정수 구간"과 "그 구간 안에서의 타이브레이크용 userLinkuId"이며,
 * 자세한 이유는 HomeRecommendScoreService#scoreBucketExpression 참고.
 */
@Slf4j
public class RecommendCursorUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private RecommendCursorUtil() {}

    /**
     * noveltyBucket/noveltyLastId, normalBucket/normalLastId는 아직 그 버킷을 한 번도 조회하지 않았으면
     * (첫 페이지) null이다 — repository의 seek 쿼리는 null을 "처음부터 조회"로 해석한다.
     */
    public record RecommendCursor(
            Integer noveltyBucket, Long noveltyLastId,
            Integer normalBucket, Long normalLastId,
            boolean noveltyExhausted) {
        public static final RecommendCursor FIRST_PAGE = new RecommendCursor(null, null, null, null, false);
    }

    /**
     * cursor가 없으면(첫 요청) 첫 페이지로 처리한다. 디코딩에 실패해도(예: 잘못된 값을 강제로 넣은 경우)
     * 에러를 던지지 않고 안전하게 첫 페이지로 폴백한다 — 외부(FE) 입력을 다루는 지점이라 원인은 로그로 남긴다
     * (utils.UrlValidUtils/infra.parser.TitleDomainParser와 동일하게, 파싱 실패를 조용히 삼키지 않는다).
     */
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

    /**
     * 필드가 고정된 몇 개의 int/long/boolean뿐이라 서버가 만드는 이 문자열 자체는 실패할 일이 없다 —
     * try-catch를 둘 실익이 없어 문자열을 직접 조립한다 (decode는 외부 입력을 다루므로 그대로 둔다).
     */
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
