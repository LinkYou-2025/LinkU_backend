package com.umc.linkyou.repository.UserLinkuRepository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.config.properties.RecommendScoreProperties;
import com.umc.linkyou.domain.QAiArticle;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.dto.RankedUsersLinku;
import com.umc.linkyou.utils.EmotionSimilarityUtil;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UsersLinkuRepositoryImpl implements UsersLinkuRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final EntityManager entityManager;
    private final RecommendScoreProperties scoreProperties;

    @Override
    public List<UsersLinku> fetchAiArticlesByCategoryId(Long userId, Long categoryId) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;
        QAiArticle aiArticle = QAiArticle.aiArticle;
        QLinku linku = QLinku.linku;

        return queryFactory
                .selectFrom(usersLinku)
                .join(usersLinku.linku, linku)
                .fetchJoin()
                .leftJoin(linku.aiArticle, aiArticle)
                .fetchJoin()
                .where(
                        usersLinku.user.id.eq(userId),
                        linku.category.categoryId.eq(categoryId),
                        usersLinku.aiExist.isTrue())
                .orderBy(usersLinku.createdAt.desc())
                .fetch();
    }

    @Override
    public List<UsersLinku> findRecentLinkCandidatesByUser(Long userId, int limit) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;

        return queryFactory
                .selectFrom(usersLinku)
                .join(usersLinku.linku)
                .fetchJoin()
                .where(
                        usersLinku.user.id.eq(userId),
                        usersLinku.createdAt.after(LocalDateTime.now().minusMonths(1)))
                .orderBy(usersLinku.createdAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<UsersLinku> fetchAiArticlesByCategoryIdWithCursor(
            Long userId, Long categoryId, Long cursorId, int limit) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;
        QLinku linku = QLinku.linku;
        QAiArticle aiArticle = QAiArticle.aiArticle;

        return queryFactory
                .selectFrom(usersLinku)
                .join(usersLinku.linku, linku)
                .fetchJoin()
                .leftJoin(linku.aiArticle, aiArticle)
                // 응답에서 읽는 emotion, domain, category만 페치 조인한다.
                .join(usersLinku.emotion)
                .fetchJoin()
                .join(linku.domain)
                .fetchJoin()
                .join(linku.category)
                .fetchJoin()
                .where(
                        usersLinku.user.id.eq(userId),
                        eqCategoryId(categoryId),
                        usersLinku.aiExist.isTrue(),
                        ltCursorId(cursorId))
                .orderBy(usersLinku.createdAt.desc(), usersLinku.userLinkuId.desc())
                .limit(limit + 1) // 다음 페이지 여부 확인용
                .fetch();
    }

    // null이면 전체 카테고리를 조회한다.
    private BooleanExpression eqCategoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return QLinku.linku.category.categoryId.eq(categoryId);
    }

    // null이나 0이면 첫 페이지다.
    private BooleanExpression ltCursorId(Long cursorId) {
        if (cursorId == null || cursorId == 0L) {
            return null;
        }
        return QUsersLinku.usersLinku.userLinkuId.lt(cursorId);
    }

    @Override
    public List<RankedUsersLinku> findNormalRecommendCandidates(
            Long userId,
            Long selectedEmotionId,
            Long selectedSituationId,
            List<Long> mappedCategoryIds,
            LocalDateTime now,
            Integer afterScoreBucket,
            Long afterUserLinkuId,
            int limit) {
        return findNormalRecommendCandidatesNative(
                userId,
                selectedEmotionId,
                selectedSituationId,
                mappedCategoryIds,
                now,
                afterScoreBucket,
                afterUserLinkuId,
                limit);
    }

    // 긴 점수식을 CTE에서 한 번만 계산하기 위해 native 쿼리를 사용
    private List<RankedUsersLinku> findNormalRecommendCandidatesNative(
            Long userId,
            Long selectedEmotionId,
            Long selectedSituationId,
            List<Long> mappedCategoryIds,
            LocalDateTime now,
            Integer afterScoreBucket,
            Long afterUserLinkuId,
            int limit) {
        String categoryCondition =
                mappedCategoryIds == null || mappedCategoryIds.isEmpty()
                        ? "FALSE"
                        : "b.category_id IN ("
                                + mappedCategoryIds.stream()
                                        .map(String::valueOf)
                                        .collect(java.util.stream.Collectors.joining(","))
                                + ")";
        String scoreExpression = nativeScoreExpression(selectedEmotionId, categoryCondition);
        boolean hasCursor = afterScoreBucket != null && afterUserLinkuId != null;
        String cursorCondition =
                hasCursor
                        ? "(score_bucket, user_linku_id) < (:afterScoreBucket, :afterUserLinkuId)"
                        : "TRUE";

        String sql =
                """
                WITH base AS (
                    SELECT
                        ul.user_linku_id,
                        l.linku_id,
                        l.category_id,
                        ul.saved_url AS linku_url,
                        l.title AS linku_title,
                        l.img_url,
                        l.total_view_count,
                        ul.memo,
                        ul.emotion_id,
                        ul.is_emotion_ai,
                        ul.situation_id,
                        ul.is_situation_ai,
                        ul.view_count,
                        ul.last_viewed_at,
                        ul.created_at,
                        ul.title AS user_title,
                        ul.image_url AS user_image_url,
                        ul.is_ai_exist,
                        d.name AS domain,
                        d.image_url AS domain_image_url
                    FROM users_linkus ul
                    JOIN linkus l ON l.linku_id = ul.linku_id
                    JOIN domains d ON d.domain_id = l.domain_id
                    WHERE ul.user_id = :userId
                ),
                scored AS MATERIALIZED (
                    SELECT
                        b.user_linku_id,
                        b.linku_id,
                        b.category_id,
                        b.linku_url,
                        b.memo,
                        b.emotion_id,
                        COALESCE(b.user_title, b.linku_title) AS title,
                        b.domain,
                        b.domain_image_url,
                        COALESCE(b.user_image_url, b.img_url) AS linku_image_url,
                        b.is_ai_exist,
                        b.last_viewed_at,
                        CAST((%s) * 200 AS integer) AS score_bucket
                    FROM base b
                )
                SELECT user_linku_id, linku_id, category_id, linku_url, memo, emotion_id,
                       title, domain, domain_image_url, linku_image_url, is_ai_exist,
                       last_viewed_at, score_bucket
                FROM scored
                WHERE %s
                ORDER BY score_bucket DESC, user_linku_id DESC
                LIMIT :limit
                """
                        .formatted(scoreExpression, cursorCondition);

        Query query =
                entityManager
                        .createNativeQuery(sql)
                        .setParameter("userId", userId)
                        .setParameter("now", now)
                        .setParameter("limit", limit)
                        .setParameter("selectedSituationId", selectedSituationId);
        if (hasCursor) {
            query.setParameter("afterScoreBucket", afterScoreBucket);
            query.setParameter("afterUserLinkuId", afterUserLinkuId);
        }

        List<RankedUsersLinku> result = new ArrayList<>();
        for (Object rowObject : query.getResultList()) {
            Object[] row = (Object[]) rowObject;
            result.add(
                    new RankedUsersLinku(
                            ((Number) row[0]).longValue(),
                            ((Number) row[1]).longValue(),
                            ((Number) row[2]).longValue(),
                            (String) row[3],
                            (String) row[4],
                            ((Number) row[5]).longValue(),
                            (String) row[6],
                            (String) row[7],
                            (String) row[8],
                            (String) row[9],
                            (Boolean) row[10],
                            row[11] instanceof Timestamp timestamp
                                    ? timestamp.toLocalDateTime()
                                    : (LocalDateTime) row[11],
                            ((Number) row[12]).intValue()));
        }
        return result;
    }

    private String nativeScoreExpression(Long selectedEmotionId, String categoryCondition) {
        RecommendScoreProperties.Weight weight = scoreProperties.weight();
        RecommendScoreProperties.Normalization normalization = scoreProperties.normalization();
        RecommendScoreProperties.Confidence confidence = scoreProperties.confidence();

        String emotion = nativeEmotionExpression(selectedEmotionId, confidence.aiEmotionDiscount());
        String situation =
                "(CASE WHEN b.situation_id = CAST(:selectedSituationId AS bigint) THEN 1.0 ELSE 0.0 END)"
                        + " * CASE WHEN b.is_situation_ai THEN "
                        + decimal(confidence.aiSituationDiscount())
                        + " ELSE 1.0 END";
        // 조회수와 마지막으로 본 지 지난 시간의 평균. 오래 안 본 링크일수록 점수가 높다.
        String engagement =
                "((LEAST(CAST(b.view_count AS double precision), "
                        + normalization.viewCountCap()
                        + ") / "
                        + normalization.viewCountCap()
                        + ") + (1.0 - EXP(-GREATEST(EXTRACT(EPOCH FROM "
                        + "(:now - COALESCE(b.last_viewed_at, b.created_at))) / 86400.0, 0) / "
                        + normalization.recencyHalfLifeDays()
                        + "))) / 2.0";
        String popularity =
                "LEAST(LN(1 + CAST(b.total_view_count AS double precision)) / LN(1 + "
                        + normalization.popularityViewCountCap()
                        + "), 1.0)";
        String category = "CASE WHEN " + categoryCondition + " THEN 1.0 ELSE 0.0 END";

        return "("
                + emotion
                + " * "
                + decimal(weight.emotion())
                + " + "
                + situation
                + " * "
                + decimal(weight.situation())
                + " + "
                + engagement
                + " * "
                + decimal(weight.engagement())
                + " + "
                + popularity
                + " * "
                + decimal(weight.popularity())
                + " + "
                + category
                + " * "
                + decimal(weight.category())
                + ")";
    }

    private String nativeEmotionExpression(Long selectedEmotionId, double aiDiscount) {
        if (selectedEmotionId == null) return "0.0";
        StringBuilder expression = new StringBuilder("(CASE b.emotion_id");
        boolean hasMatch = false;
        for (long candidateEmotionId = 1; candidateEmotionId <= 6; candidateEmotionId++) {
            int similarity =
                    EmotionSimilarityUtil.getSimilarityScore(selectedEmotionId, candidateEmotionId);
            if (similarity > 0) {
                hasMatch = true;
                expression
                        .append(" WHEN ")
                        .append(candidateEmotionId)
                        .append(" THEN ")
                        .append(similarity);
            }
        }
        if (!hasMatch) return "0.0";
        return expression
                .append(" ELSE 0 END / 60.0) * CASE WHEN b.is_emotion_ai THEN ")
                .append(decimal(aiDiscount))
                .append(" ELSE 1.0 END")
                .toString();
    }

    private String decimal(double value) {
        return String.format(Locale.ROOT, "%.12f", value);
    }
}
