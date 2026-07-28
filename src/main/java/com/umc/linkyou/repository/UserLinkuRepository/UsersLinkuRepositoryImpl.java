package com.umc.linkyou.repository.UserLinkuRepository;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QAiArticle;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.dto.RankedUsersLinku;
import com.umc.linkyou.service.common.HomeRecommendScoreService;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class UsersLinkuRepositoryImpl implements UsersLinkuRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final HomeRecommendScoreService homeRecommendScoreService;

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
                .leftJoin(linku.aiArticle, aiArticle).fetchJoin()
                // 서비스단(getMyAiArticlesByCategory)에서 ul.getEmotion()/l.getDomain()을 사용하므로
                // 추가 쿼리(N+1)를 막기 위해 emotion/domain도 함께 페치 조인한다. 둘 다 not-null 연관관계.
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
        QUsersLinku usersLinku = QUsersLinku.usersLinku;
        QLinku linku = QLinku.linku;
        QAiArticle aiArticle = QAiArticle.aiArticle;
        BooleanExpression excludeNovelty =
                homeRecommendScoreService.notNoveltyCondition(usersLinku, now, recencyThresholdDays);

        NumberExpression<Double> totalScore = homeRecommendScoreService.scoreExpression(
                usersLinku, linku, aiArticle, userId, selectedEmotionId, selectedSituationId, mappedCategoryIds,
                now, profileTsqueryText, profileText);
        NumberExpression<Integer> bucket = homeRecommendScoreService.scoreBucketExpression(totalScore);
        BooleanExpression seek = seekCondition(bucket, usersLinku, afterScoreBucket, afterUserLinkuId);

        JPAQuery<Tuple> query = queryFactory
                .select(usersLinku, bucket)
                .from(usersLinku)
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
                .where(usersLinku.user.id.eq(userId), excludeNovelty, seek);

        List<Tuple> rows = query
                // scoreBucket이 성긴 정수 구간이라 그 안에서는 userLinkuId(단조 증가)로 타이브레이크한다 —
                // scoreBucketExpression() javadoc 참고.
                .orderBy(bucket.desc(), usersLinku.userLinkuId.desc())
                .limit(limit)
                .fetch();

        return toRankedList(rows, usersLinku, bucket);
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
        BooleanExpression noveltyCondition =
                homeRecommendScoreService.noveltyCondition(usersLinku, now, recencyThresholdDays);
        BooleanExpression seek = seekCondition(bucket, usersLinku, afterScoreBucket, afterUserLinkuId);

        List<Tuple> rows = queryFactory
                .select(usersLinku, bucket)
                .from(usersLinku)
                .join(usersLinku.linku, linku).fetchJoin()
                .join(usersLinku.emotion).fetchJoin()
                .join(linku.category).fetchJoin()
                .join(linku.domain).fetchJoin()
                .leftJoin(usersLinku.situation)
                .where(usersLinku.user.id.eq(userId), noveltyCondition, seek)
                .orderBy(bucket.desc(), usersLinku.userLinkuId.desc())
                .limit(limit)
                .fetch();

        return toRankedList(rows, usersLinku, bucket);
    }

    /**
     * seek(keyset) 탐색 조건. afterScoreBucket/afterUserLinkuId가 둘 다 null이면(첫 페이지) 조건 없이
     * 처음부터 가져온다. 아니면 정렬 기준(bucket DESC, userLinkuId DESC)과 짝을 맞춰
     * (bucket, userLinkuId)가 튜플 기준으로 그 지점보다 "작은" 행부터 가져온다 — OFFSET처럼 앞부분을
     * 다시 스캔/스킵하지 않는다.
     */
    private BooleanExpression seekCondition(
            NumberExpression<Integer> bucket, QUsersLinku usersLinku,
            Integer afterScoreBucket, Long afterUserLinkuId) {
        if (afterScoreBucket == null || afterUserLinkuId == null) {
            return null;
        }
        return bucket.lt(afterScoreBucket)
                .or(bucket.eq(afterScoreBucket).and(usersLinku.userLinkuId.lt(afterUserLinkuId)));
    }

    /** Tuple(usersLinku, bucket) 결과를 RankedUsersLinku 리스트로 변환한다. */
    private List<RankedUsersLinku> toRankedList(
            List<Tuple> rows, QUsersLinku usersLinku, NumberExpression<Integer> bucket) {
        List<RankedUsersLinku> result = new ArrayList<>(rows.size());
        for (Tuple row : rows) {
            result.add(new RankedUsersLinku(row.get(usersLinku), row.get(bucket)));
        }
        return result;
    }

    /**
     * findHomeRecommendCandidates 전용 쿼리(OFFSET 기반). findNormalRecommendCandidates는 seek(keyset)
     * 기반으로 전환됐지만, 이 메서드는 기존 호출부(테스트 등)와의 하위호환을 위해 그대로 둔다 —
     * 정렬 기준이 바뀌면 안 되는 별도 메서드이므로 위 seek 쿼리와 통합하지 않았다.
     */
    private List<UsersLinku> queryRankedCandidates(
            Long userId, Long selectedEmotionId, Long selectedSituationId, List<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText,
            BooleanExpression extraCondition, int offset, int limit) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;
        QLinku linku = QLinku.linku;
        QAiArticle aiArticle = QAiArticle.aiArticle;

        NumberExpression<Double> totalScore = homeRecommendScoreService.scoreExpression(
                usersLinku, linku, aiArticle, userId, selectedEmotionId, selectedSituationId, mappedCategoryIds,
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
