package com.umc.linkyou.repository.UserLinkuRepository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.config.properties.RecommendScoreProperties;
import com.umc.linkyou.domain.QAiArticle;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.dto.RankedUsersLinku;
import com.umc.linkyou.service.common.HomeRecommendScoreService;
import com.umc.linkyou.utils.EmotionSimilarityUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class UsersLinkuRepositoryImpl implements UsersLinkuRepositoryCustom {

    private static final String SCORE_BUCKET_ALIAS = "scoreBucket";

    private final JPAQueryFactory queryFactory;
    private final HomeRecommendScoreService homeRecommendScoreService;
    private final EntityManager entityManager;
    private final RecommendScoreProperties scoreProperties;

    @Override
    public List<UsersLinku> fetchAiArticlesByCategoryId(Long userId, Long categoryId) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;
        QAiArticle aiArticle = QAiArticle.aiArticle;
        QLinku linku = QLinku.linku;

        return queryFactory
                .selectFrom(usersLinku)
                // UsersLinku -> Linku (ManyToOne) 페치 조인
                .join(usersLinku.linku, linku).fetchJoin()
                // Linku -> AiArticle (OneToOne) 페치 조인
                .leftJoin(linku.aiArticle, aiArticle).fetchJoin()
                .where(
                        usersLinku.user.id.eq(userId),
                        linku.category.categoryId.eq(categoryId), // QLinku 내부의 category 참조
                        usersLinku.aiExist.isTrue() // AI 분석 데이터가 존재하는 것만
                )
                .orderBy(usersLinku.createdAt.desc())
                .fetch();
    }

    @Override
    public List<UsersLinku> findRecentLinkCandidatesByUser(Long userId, int limit) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;

        return queryFactory
                .selectFrom(usersLinku)
                .join(usersLinku.linku).fetchJoin()
                .where(
                        usersLinku.user.id.eq(userId),
                        usersLinku.createdAt.after(LocalDateTime.now().minusMonths(1))
                )
                .orderBy(usersLinku.createdAt.desc())
                .limit(limit)
                .fetch();
    }
    @Override
    public List<UsersLinku> fetchAiArticlesByCategoryIdWithCursor(Long userId, Long categoryId, Long cursorId, int limit) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;
        QLinku linku = QLinku.linku;
        QAiArticle aiArticle = QAiArticle.aiArticle;

        return queryFactory
                .selectFrom(usersLinku)
                .join(usersLinku.linku, linku).fetchJoin()
                .leftJoin(linku.aiArticle, aiArticle)
                // 서비스단(getMyAiArticlesByCategory)에서 ul.getEmotion()/l.getDomain()을 사용하므로
                // emotion/domain은 페치 조인하되, AiArticle은 응답에서 읽지 않아 일반 LEFT JOIN으로 둔다.
                .join(usersLinku.emotion).fetchJoin()
                .join(linku.domain).fetchJoin()
                .where(
                        usersLinku.user.id.eq(userId),
                        linku.category.categoryId.eq(categoryId),
                        usersLinku.aiExist.isTrue(),
                        ltCursorId(cursorId) // 커서 조건 추가
                )
                .orderBy(usersLinku.createdAt.desc(), usersLinku.userLinkuId.desc()) // 정렬 순서 보장
                .limit(limit + 1) // 다음 페이지 여부 확인용
                .fetch();
    }

    // 커서 조건 처리 (최신순이므로 현재 커서보다 작은 ID를 가져옴)
    private BooleanExpression ltCursorId(Long cursorId) {
        if (cursorId == null || cursorId == 0L) {
            return null;
        }
        return QUsersLinku.usersLinku.userLinkuId.lt(cursorId);
    }

    @Override
    public List<UsersLinku> findHomeRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText, int offset, int limit) {
        return queryRankedCandidates(userId, selectedEmotionId, selectedSituationId, mappedCategoryIds,
                now, profileTsqueryText, profileText, null, offset, limit);
    }

    @Override
    public List<RankedUsersLinku> findNormalRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText,
            int recencyThresholdDays, Integer afterScoreBucket, Long afterUserLinkuId, int limit) {
        return findNormalRecommendCandidatesNative(
                userId, selectedEmotionId, selectedSituationId, mappedCategoryIds, now,
                profileTsqueryText, profileText, recencyThresholdDays, afterScoreBucket,
                afterUserLinkuId, limit);
    }

    //  점수식을 CTE에서 한 번만 계산해 HQL 파서의 대형 표현식 중복을 피하는 Native 쿼리 방식 조회
    private List<RankedUsersLinku> findNormalRecommendCandidatesNative(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText,
            int recencyThresholdDays, Integer afterScoreBucket, Long afterUserLinkuId, int limit) {
        String categoryCondition = mappedCategoryIds == null || mappedCategoryIds.isEmpty()
                ? "FALSE"
                : "l.category_id IN (" + mappedCategoryIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ")";
        String scoreExpression = nativeScoreExpression(
                selectedEmotionId, selectedSituationId, categoryCondition, profileTsqueryText, profileText);
        boolean hasCursor = afterScoreBucket != null && afterUserLinkuId != null;
        String cursorCondition = hasCursor
                ? "(score_bucket, user_linku_id) < (:afterScoreBucket, :afterUserLinkuId)"
                : "TRUE";

        String sql = """
                WITH scored AS MATERIALIZED (
                    SELECT
                        ul.user_linku_id,
                        l.linku_id,
                        l.category_id,
                        l.linku_url,
                        ul.memo,
                        ul.emotion_id,
                        COALESCE(ul.title, l.title) AS title,
                        d.name AS domain,
                        d.image_url AS domain_image_url,
                        COALESCE(ul.image_url, l.img_url) AS linku_image_url,
                        ul.is_ai_exist,
                        ul.last_viewed_at,
                        CAST((%s) * 200 AS integer) AS score_bucket
                    FROM users_linkus ul
                    JOIN linkus l ON l.linku_id = ul.linku_id
                    JOIN domains d ON d.domain_id = l.domain_id
                    LEFT JOIN ai_articles aa ON aa.linku_id = l.linku_id
                    WHERE ul.user_id = :userId
                      AND NOT (COALESCE(ul.last_viewed_at, ul.created_at) < :noveltyThreshold)
                )
                SELECT user_linku_id, linku_id, category_id, linku_url, memo, emotion_id,
                       title, domain, domain_image_url, linku_image_url, is_ai_exist,
                       last_viewed_at, score_bucket
                FROM scored
                WHERE %s
                ORDER BY score_bucket DESC, user_linku_id DESC
                LIMIT :limit
                """.formatted(scoreExpression, cursorCondition);

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("noveltyThreshold", now.minusDays(recencyThresholdDays))
                .setParameter("now", now)
                .setParameter("limit", limit)
                .setParameter("selectedSituationId", selectedSituationId);
        if (hasCursor) {
            query.setParameter("afterScoreBucket", afterScoreBucket);
            query.setParameter("afterUserLinkuId", afterUserLinkuId);
        }
        if (profileTsqueryText != null || profileText != null) {
            query.setParameter("profileTsqueryText", profileTsqueryText);
            query.setParameter("profileText", profileText);
        }

        List<RankedUsersLinku> result = new ArrayList<>();
        for (Object rowObject : query.getResultList()) {
            Object[] row = (Object[]) rowObject;
            result.add(new RankedUsersLinku(
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
                    row[11] instanceof Timestamp timestamp ? timestamp.toLocalDateTime() : (LocalDateTime) row[11],
                    ((Number) row[12]).intValue()));
        }
        return result;
    }

    private String nativeScoreExpression(
            Long selectedEmotionId, Long selectedSituationId, String categoryCondition,
            String profileTsqueryText, String profileText) {
        RecommendScoreProperties.Weight weight = scoreProperties.weight();
        RecommendScoreProperties.Normalization normalization = scoreProperties.normalization();
        RecommendScoreProperties.Confidence confidence = scoreProperties.confidence();

        String emotion = nativeEmotionExpression(selectedEmotionId, confidence.aiEmotionDiscount());
        String situation = "(CASE WHEN ul.situation_id = CAST(:selectedSituationId AS bigint) THEN 1.0 ELSE 0.0 END)"
                + " * CASE WHEN ul.is_situation_ai THEN " + decimal(confidence.aiSituationDiscount()) + " ELSE 1.0 END";
        String engagement = "((LEAST(CAST(ul.view_count AS double precision), " + normalization.viewCountCap() + ") / "
                + normalization.viewCountCap() + ") + CASE WHEN ul.last_viewed_at IS NULL THEN 0.0 ELSE "
                + "EXP(-GREATEST(EXTRACT(EPOCH FROM (:now - ul.last_viewed_at)) / 86400.0, 0) / "
                + normalization.recencyHalfLifeDays() + ") END) / 2.0";
        String popularity = "LEAST(LN(1 + CAST(l.total_view_count AS double precision)) / LN(1 + "
                + normalization.popularityViewCountCap() + "), 1.0)";
        String text = nativeTextExpression(profileTsqueryText, profileText);
        String keyword = "LEAST(CAST(COALESCE((SELECT SUM(upk.weight) FROM linku_keywords lk "
                + "JOIN user_profile_keywords upk ON upk.keyword_id = lk.keyword_id "
                + "WHERE lk.linku_id = l.linku_id AND upk.user_id = :userId), 0) AS double precision), "
                + normalization.keywordWeightCap() + ") / " + normalization.keywordWeightCap();
        String category = "CASE WHEN " + categoryCondition + " THEN 1.0 ELSE 0.0 END";

        return "(" + emotion + " * " + decimal(weight.emotion())
                + " + " + situation + " * " + decimal(weight.situation())
                + " + " + engagement + " * " + decimal(weight.engagement())
                + " + " + popularity + " * " + decimal(weight.popularity())
                + " + " + text + " * " + decimal(weight.text())
                + " + " + keyword + " * " + decimal(weight.keyword())
                + " + " + category + " * " + decimal(weight.category()) + ")";
    }

    private String nativeEmotionExpression(Long selectedEmotionId, double aiDiscount) {
        if (selectedEmotionId == null) return "0.0";
        StringBuilder expression = new StringBuilder("(CASE ul.emotion_id");
        boolean hasMatch = false;
        for (long candidateEmotionId = 1; candidateEmotionId <= 6; candidateEmotionId++) {
            int similarity = EmotionSimilarityUtil.getSimilarityScore(selectedEmotionId, candidateEmotionId);
            if (similarity > 0) {
                hasMatch = true;
                expression.append(" WHEN ").append(candidateEmotionId).append(" THEN ").append(similarity);
            }
        }
        if (!hasMatch) return "0.0";
        return expression.append(" ELSE 0 END / 60.0) * CASE WHEN ul.is_emotion_ai THEN ")
                .append(decimal(aiDiscount)).append(" ELSE 1.0 END").toString();
    }

    private String nativeTextExpression(String profileTsqueryText, String profileText) {
        if (profileTsqueryText == null && profileText == null) return "0.0";
        String document = "to_tsvector('simple', l.title || ' ' || COALESCE(aa.summary, ''))";
        String fts = "CAST(ts_rank_cd(" + document + ", to_tsquery('simple', :profileTsqueryText)) AS double precision)";
        String trgm = "COALESCE(similarity(l.title || ' ' || COALESCE(aa.summary, ''), :profileText), 0)";
        return "CASE WHEN :profileTsqueryText IS NULL OR :profileTsqueryText = '' THEN 0.0 "
                + "WHEN " + fts + " > 0 THEN " + fts + " ELSE " + trgm + " * 0.7 END";
    }

    private String decimal(double value) {
        return String.format(Locale.ROOT, "%.12f", value);
    }

    @Override
    public List<RankedUsersLinku> findNoveltyRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId,
            LocalDateTime now, int recencyThresholdDays, Integer afterScoreBucket, Long afterUserLinkuId, int limit) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;
        QLinku linku = QLinku.linku;
        NumberExpression<Double> contextScore = homeRecommendScoreService
                .noveltyContextScoreExpression(usersLinku, selectedEmotionId, selectedSituationId);
        NumberExpression<Integer> bucket = homeRecommendScoreService.scoreBucketExpression(contextScore);
        NumberPath<Integer> bucketAlias = Expressions.numberPath(Integer.class, SCORE_BUCKET_ALIAS);
        NumberExpression<Integer> selectedBucket = bucket.as(bucketAlias);
        BooleanExpression noveltyCondition =
                homeRecommendScoreService.noveltyCondition(usersLinku, now, recencyThresholdDays);
        BooleanExpression seek = seekCondition(bucket, usersLinku, afterScoreBucket, afterUserLinkuId);

        return queryFactory
                .select(Projections.constructor(
                        RankedUsersLinku.class,
                        usersLinku.userLinkuId,
                        linku.linkuId,
                        linku.category.categoryId,
                        linku.linkuUrl,
                        usersLinku.memo,
                        usersLinku.emotion.emotionId,
                        usersLinku.title.coalesce(linku.title),
                        linku.domain.name,
                        linku.domain.imageUrl,
                        usersLinku.imageUrl.coalesce(linku.imgUrl),
                        usersLinku.aiExist,
                        usersLinku.lastViewedAt,
                        selectedBucket))
                .from(usersLinku)
                .join(usersLinku.linku, linku)
                .join(usersLinku.emotion)
                .join(linku.category)
                .join(linku.domain)
                .leftJoin(usersLinku.situation)
                .where(usersLinku.user.id.eq(userId), noveltyCondition, seek)
                .orderBy(bucketAlias.desc(), usersLinku.userLinkuId.desc())
                .limit(limit)
                .fetch();
    }

    // seek 탐색 조건으로, 둘다 null이면 조건 없음으로 첫 페이지로, 아니라면 이전 행부터 탐색
    private BooleanExpression seekCondition(
            NumberExpression<Integer> bucket, QUsersLinku usersLinku,
            Integer afterScoreBucket, Long afterUserLinkuId) {
        if (afterScoreBucket == null || afterUserLinkuId == null) {
            return null;
        }
        // 복합 커서를 튜플로 비교해 bucket 표현식을 WHERE에 한 번만 사용한다.
        return Expressions.booleanTemplate(
                "({0}, {1}) < ({2}, {3})",
                bucket, usersLinku.userLinkuId, afterScoreBucket, afterUserLinkuId);
    }

    /** findHomeRecommendCandidates 전용 OFFSET 쿼리 — 하위호환용으로 유지, seek 쿼리와 통합 안 함 */
    private List<UsersLinku> queryRankedCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText,
            BooleanExpression extraCondition, int offset, int limit) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;
        QLinku linku = QLinku.linku;
        QAiArticle aiArticle = QAiArticle.aiArticle;

        NumberExpression<Double> totalScore = homeRecommendScoreService.scoreExpression(
                usersLinku, linku, aiArticle, userId,
                selectedEmotionId, selectedSituationId, mappedCategoryIds,
                now, profileTsqueryText, profileText);

        var query = queryFactory
                .selectFrom(usersLinku)
                // 감정/링크/카테고리/도메인을 한 번에 fetch join 해서 N+1을 없앤다.
                .join(usersLinku.linku, linku).fetchJoin()
                .join(usersLinku.emotion).fetchJoin()
                .join(linku.category).fetchJoin()
                .join(linku.domain).fetchJoin()
                // situation은 nullable이라 fetchJoin 없이 LEFT JOIN만 건다.
                // (INNER JOIN이나 조인 없이 경로만 참조하면 situation이 없는 저장 링크가 통째로 빠지는 버그가 생김 —
                //  HomeRecommendScoreService#scoreExpression 주석 참고)
                .leftJoin(usersLinku.situation)
                // summary는 없는 링크도 있어서 LEFT JOIN. fetchJoin은 안 걸었다 — TextMatch 계산에만 쓰이고
                // Java 쪽에서 linku.getAiArticle()을 다시 꺼내 쓰지 않는다.
                .leftJoin(linku.aiArticle, aiArticle)
                .where(usersLinku.user.id.eq(userId));

        if (extraCondition != null) {
            query = query.where(extraCondition);
        }

        return query
                // 점수/정렬/페이징을 전부 DB에서 처리 (애플리케이션 메모리로 전체 로드하지 않음)
                .orderBy(totalScore.desc(), usersLinku.createdAt.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    @Override
    public List<UsersLinku> findRecentContentForProfile(Long userId, int limit) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;
        QLinku linku = QLinku.linku;
        QAiArticle aiArticle = QAiArticle.aiArticle;

        return queryFactory
                .selectFrom(usersLinku)
                .join(usersLinku.linku, linku).fetchJoin()
                // summary 없는 링크도 있으니 LEFT JOIN
                .leftJoin(linku.aiArticle, aiArticle).fetchJoin()
                .where(usersLinku.user.id.eq(userId))
                .orderBy(usersLinku.createdAt.desc())
                .limit(limit)
                .fetch();
    }
}
