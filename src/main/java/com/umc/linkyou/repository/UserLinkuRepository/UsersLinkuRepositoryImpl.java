package com.umc.linkyou.repository.UserLinkuRepository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.config.properties.RecommendScoreProperties;
import com.umc.linkyou.domain.QAiArticle;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.dto.RankedUsersLinku;
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
                // 서비스단(getMyAiArticlesByCategory)에서 ul.getEmotion()/l.getDomain()/l.getCategory()를
                // 사용하므로 emotion/domain/category는 페치 조인하되, AiArticle은 응답에서 읽지 않아
                // 일반 LEFT JOIN으로 둔다.
                .join(usersLinku.emotion).fetchJoin()
                .join(linku.domain).fetchJoin()
                .join(linku.category).fetchJoin()
                .where(
                        usersLinku.user.id.eq(userId),
                        eqCategoryId(categoryId), // null이면 카테고리 필터 없이 전체 조회
                        usersLinku.aiExist.isTrue(),
                        ltCursorId(cursorId) // 커서 조건 추가
                )
                .orderBy(usersLinku.createdAt.desc(), usersLinku.userLinkuId.desc()) // 정렬 순서 보장
                .limit(limit + 1) // 다음 페이지 여부 확인용
                .fetch();
    }

    // categoryId가 없으면(=전체 카테고리 조회) 조건을 걸지 않는다
    private BooleanExpression eqCategoryId(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return QLinku.linku.category.categoryId.eq(categoryId);
    }

    // 커서 조건 처리 (최신순이므로 현재 커서보다 작은 ID를 가져옴)
    private BooleanExpression ltCursorId(Long cursorId) {
        if (cursorId == null || cursorId == 0L) {
            return null;
        }
        return QUsersLinku.usersLinku.userLinkuId.lt(cursorId);
    }

    @Override
    public List<RankedUsersLinku> findNormalRecommendCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText,
            Integer afterScoreBucket, Long afterUserLinkuId, int limit) {
        return findNormalRecommendCandidatesNative(
                userId, selectedEmotionId, selectedSituationId, mappedCategoryIds, now,
                profileTsqueryText, profileText, afterScoreBucket,
                afterUserLinkuId, limit);
    }

    //  점수식을 CTE에서 한 번만 계산해 HQL 파서의 대형 표현식 중복을 피하는 Native 쿼리 방식 조회
    private List<RankedUsersLinku> findNormalRecommendCandidatesNative(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText,
            Integer afterScoreBucket, Long afterUserLinkuId, int limit) {
        String categoryCondition = mappedCategoryIds == null || mappedCategoryIds.isEmpty()
                ? "FALSE"
                : "b.category_id IN (" + mappedCategoryIds.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")) + ")";
        String scoreExpression = nativeScoreExpression(
                selectedEmotionId, selectedSituationId, categoryCondition, profileTsqueryText, profileText);
        boolean hasCursor = afterScoreBucket != null && afterUserLinkuId != null;
        String cursorCondition = hasCursor
                ? "(score_bucket, user_linku_id) < (:afterScoreBucket, :afterUserLinkuId)"
                : "TRUE";

        // base: join + fts_rank(ts_rank_cd/to_tsvector)를 행당 딱 한 번만 계산해서 컬럼으로 노출한다.
        // 예전엔 nativeTextExpression()이 이 계산식 텍스트를 CASE 절 안에서 그대로 4번 반복 삽입해서
        // (WHEN/THEN 각 2회 참조 x 분자/분모 각 2회 참조) 후보 행마다 to_tsvector/ts_rank_cd가 4번씩
        // 실행됐다 — 이게 추천 조회가 느렸던 주 원인. base에서 한 번 계산해 fts_rank 컬럼으로 두면,
        // scored에서는 그 값을 몇 번 참조하든 컬럼 읽기일 뿐 함수가 재실행되지 않는다.
        //
        // fts_rank 컬럼식 자체는 nativeFtsRankColumn()으로 뽑아뒀다 — profileTsqueryText/profileText가
        // 둘 다 null(신규 유저 등 프로필 미생성)이면 아래에서 :profileTsqueryText 파라미터를 아예
        // 바인딩하지 않는데, 이 컬럼식을 무조건 CASE ... :profileTsqueryText ... 로 박아두면 파라미터
        // 미바인딩 예외가 난다. nativeTextExpression()과 동일한 null 체크로 맞춰서, 그 경우엔 SQL에도
        // 플레이스홀더 자체를 넣지 않는다(NULL 리터럴만 사용).
        String ftsRankColumn = nativeFtsRankColumn(profileTsqueryText, profileText);
        String sql = """
                WITH base AS (
                    SELECT
                        ul.user_linku_id,
                        l.linku_id,
                        l.category_id,
                        l.linku_url,
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
                        d.image_url AS domain_image_url,
                        aa.summary,
                        %s AS fts_rank
                    FROM users_linkus ul
                    JOIN linkus l ON l.linku_id = ul.linku_id
                    JOIN domains d ON d.domain_id = l.domain_id
                    LEFT JOIN ai_articles aa ON aa.linku_id = l.linku_id
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
                """.formatted(ftsRankColumn, scoreExpression, cursorCondition);

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
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
        String situation = "(CASE WHEN b.situation_id = CAST(:selectedSituationId AS bigint) THEN 1.0 ELSE 0.0 END)"
                + " * CASE WHEN b.is_situation_ai THEN " + decimal(confidence.aiSituationDiscount()) + " ELSE 1.0 END";
        // staleness: COALESCE(last_viewed_at, created_at)가 오래될수록 1에 가까워진다 — 최근에 보거나
        // 만든 것을 우대하던 예전 recency 항의 부호를 뒤집은 것(예전 Novelty Quota의 하드 제외 필터를
        // 이 축의 연속값으로 대체). half-life는 기존 recencyHalfLifeDays를 그대로 재사용한다.
        String engagement = "((LEAST(CAST(b.view_count AS double precision), " + normalization.viewCountCap() + ") / "
                + normalization.viewCountCap() + ") + (1.0 - EXP(-GREATEST(EXTRACT(EPOCH FROM "
                + "(:now - COALESCE(b.last_viewed_at, b.created_at))) / 86400.0, 0) / "
                + normalization.recencyHalfLifeDays() + "))) / 2.0";
        String popularity = "LEAST(LN(1 + CAST(b.total_view_count AS double precision)) / LN(1 + "
                + normalization.popularityViewCountCap() + "), 1.0)";
        String text = nativeTextExpression(profileTsqueryText, profileText);
        String keyword = "LEAST(CAST(COALESCE((SELECT SUM(upk.weight) FROM linku_keywords lk "
                + "JOIN user_profile_keywords upk ON upk.keyword_id = lk.keyword_id "
                + "WHERE lk.linku_id = b.linku_id AND upk.user_id = :userId), 0) AS double precision), "
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
        StringBuilder expression = new StringBuilder("(CASE b.emotion_id");
        boolean hasMatch = false;
        for (long candidateEmotionId = 1; candidateEmotionId <= 6; candidateEmotionId++) {
            int similarity = EmotionSimilarityUtil.getSimilarityScore(selectedEmotionId, candidateEmotionId);
            if (similarity > 0) {
                hasMatch = true;
                expression.append(" WHEN ").append(candidateEmotionId).append(" THEN ").append(similarity);
            }
        }
        if (!hasMatch) return "0.0";
        return expression.append(" ELSE 0 END / 60.0) * CASE WHEN b.is_emotion_ai THEN ")
                .append(decimal(aiDiscount)).append(" ELSE 1.0 END").toString();
    }

    // base CTE의 fts_rank 컬럼식. profileTsqueryText/profileText가 둘 다 없으면(신규 유저 등 프로필
    // 미생성 상태) findNormalRecommendCandidatesNative()가 :profileTsqueryText 파라미터를 바인딩하지
    // 않으므로, 그 경우엔 SQL에도 이 플레이스홀더를 아예 넣지 않고 NULL 리터럴만 반환한다 — 안 그러면
    // 파라미터 미바인딩 예외가 난다. nativeTextExpression()의 null 체크와 반드시 같은 조건이어야 한다.
    private String nativeFtsRankColumn(String profileTsqueryText, String profileText) {
        if (profileTsqueryText == null && profileText == null) {
            return "NULL";
        }
        // GIN 인덱스(title_tsv/summary_tsv generated column)는 도입했다가 뺐다 — @@ 검색 조건 없이
        // ts_rank_cd로 랭킹만 계산하는 지금 쿼리 형태에서는 GIN 인덱스가 실행계획에 전혀 안 잡혀서
        // (@@가 있어야 GIN이 쓰인다) 실익 없이 컬럼/인덱스만 늘리는 꼴이었다. to_tsvector를 즉석
        // 계산하되, base CTE에서 한 번만 계산해두는 것만으로 충분하다(행마다 4번 계산되던 예전 버그만
        // 고치면 됨 — 아래 nativeTextExpression() 주석 참고).
        return """
                CASE WHEN :profileTsqueryText IS NULL OR :profileTsqueryText = '' THEN NULL
                     ELSE CAST(ts_rank_cd(
                         to_tsvector('simple', l.title || ' ' || COALESCE(aa.summary, '')),
                         to_tsquery('simple', :profileTsqueryText)) AS double precision)
                END""";
    }

    // fts_rank(ts_rank_cd/to_tsvector)는 base CTE에서 이미 한 번 계산해 컬럼(b.fts_rank)으로 넘어온다.
    // 여기서 b.fts_rank를 CASE 안에서 여러 번 참조해도 컬럼 값을 읽는 것뿐이라 함수가 재실행되지 않는다
    // (예전 버전은 to_tsvector/ts_rank_cd 호출 텍스트 자체를 이 메서드 안에서 4번 반복 삽입해서
    // 후보 행마다 4번씩 실행됐다 — 추천 조회가 느렸던 주 원인).
    private String nativeTextExpression(String profileTsqueryText, String profileText) {
        if (profileTsqueryText == null && profileText == null) return "0.0";
        String trgm = "COALESCE(similarity(b.linku_title || ' ' || COALESCE(b.summary, ''), :profileText), 0)";
        return "CASE WHEN :profileTsqueryText IS NULL OR :profileTsqueryText = '' THEN 0.0 "
                + "WHEN b.fts_rank IS NOT NULL AND b.fts_rank > 0 THEN (b.fts_rank / (b.fts_rank + 1.0)) "
                + "ELSE " + trgm + " * 0.7 END";
    }

    private String decimal(double value) {
        return String.format(Locale.ROOT, "%.12f", value);
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
