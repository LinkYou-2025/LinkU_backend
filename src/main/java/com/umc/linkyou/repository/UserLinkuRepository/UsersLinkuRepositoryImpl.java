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
                .join(usersLinku.linku, linku).fetchJoin()
                .join(usersLinku.emotion).fetchJoin()
                .join(linku.category).fetchJoin()
                .join(linku.domain).fetchJoin()
                // situation nullable — INNER/경로참조만 하면 situation 없는 링크가 빠지는 버그가 생김
                .leftJoin(usersLinku.situation)
                // fetchJoin 필수: AiArticle.linku 가 owning side라 Linku.aiArticle(mappedBy, LAZY)은
                // 진짜 지연로딩이 안 되고(프록시를 못 만들어서) 엔티티 로드 시점에 무조건 존재 확인
                // 쿼리를 한 번 더 던진다 — fetchJoin으로 여기서 같이 실어야 결과 row당 N+1이 안 생김.
                .leftJoin(linku.aiArticle, aiArticle).fetchJoin()
                .where(usersLinku.user.id.eq(userId), excludeNovelty, seek);

        List<Tuple> rows = query
                // bucket 내 동점은 userLinkuId로 타이브레이크
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
        QAiArticle aiArticle = QAiArticle.aiArticle;

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
                // findNormalRecommendCandidates와 동일한 이유로 fetchJoin 필요 (AiArticle이 owning
                // side라 Linku.aiArticle이 진짜 지연로딩 안 됨 — row당 존재확인 쿼리 N+1 방지).
                .leftJoin(linku.aiArticle, aiArticle).fetchJoin()
                .where(usersLinku.user.id.eq(userId), noveltyCondition, seek)
                .orderBy(bucket.desc(), usersLinku.userLinkuId.desc())
                .limit(limit)
                .fetch();

        return toRankedList(rows, usersLinku, bucket);
    }

    /** seek 탐색 조건. 둘 다 null이면 첫 페이지(조건 없음), 아니면 (bucket, userLinkuId) 이전 행부터 */
    private BooleanExpression seekCondition(
            NumberExpression<Integer> bucket, QUsersLinku usersLinku,
            Integer afterScoreBucket, Long afterUserLinkuId) {
        if (afterScoreBucket == null || afterUserLinkuId == null) {
            return null;
        }
        return bucket.lt(afterScoreBucket)
                .or(bucket.eq(afterScoreBucket).and(usersLinku.userLinkuId.lt(afterUserLinkuId)));
    }

    /** Tuple(usersLinku, bucket) → RankedUsersLinku 변환 */
    private List<RankedUsersLinku> toRankedList(
            List<Tuple> rows, QUsersLinku usersLinku, NumberExpression<Integer> bucket) {
        List<RankedUsersLinku> result = new ArrayList<>(rows.size());
        for (Tuple row : rows) {
            result.add(new RankedUsersLinku(row.get(usersLinku), row.get(bucket)));
        }
        return result;
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
