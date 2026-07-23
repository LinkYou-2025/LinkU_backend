package com.umc.linkyou.repository.UserLinkuRepository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QAiArticle;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.utils.EmotionSimilarityUtil;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class UsersLinkuRepositoryImpl implements UsersLinkuRepositoryCustom {

    private final JPAQueryFactory queryFactory;

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
            Long userId, Long selectedEmotionId, List<Long> mappedCategoryIds, int offset, int limit) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;
        QLinku linku = QLinku.linku;

        NumberExpression<Integer> totalScore = emotionScoreExpression(usersLinku, selectedEmotionId)
                .add(situationScoreExpression(linku, mappedCategoryIds));

        return queryFactory
                .selectFrom(usersLinku)
                // 감정/링크/카테고리/도메인을 한 번에 fetch join 해서 N+1을 없앤다.
                .join(usersLinku.linku, linku).fetchJoin()
                .join(usersLinku.emotion).fetchJoin()
                .join(linku.category).fetchJoin()
                .join(linku.domain).fetchJoin()
                .where(usersLinku.user.id.eq(userId))
                // 점수/정렬/페이징을 전부 DB에서 처리 (애플리케이션 메모리로 전체 로드하지 않음)
                .orderBy(totalScore.desc(), usersLinku.createdAt.desc())
                .offset(offset)
                .limit(limit)
                .fetch();
    }

    // 감정 유사도 점수를 CASE WHEN 식으로 변환한다.
    // EmotionSimilarityUtil의 (선택 감정, 후보 감정) 매핑을 그대로 SQL로 옮긴 것으로,
    // 실제 점수 값의 출처는 여전히 EmotionSimilarityUtil 하나다.
    private NumberExpression<Integer> emotionScoreExpression(QUsersLinku usersLinku, Long selectedEmotionId) {
        CaseBuilder.Cases<Integer, NumberExpression<Integer>> chain = null;

        for (long candidateEmotionId = 1; candidateEmotionId <= 6; candidateEmotionId++) {
            int score = EmotionSimilarityUtil.getSimilarityScore(selectedEmotionId, candidateEmotionId);
            if (score == 0) {
                continue;
            }
            BooleanExpression condition = usersLinku.emotion.emotionId.eq(candidateEmotionId);
            chain = (chain == null)
                    ? new CaseBuilder().when(condition).then(score)
                    : chain.when(condition).then(score);
        }

        return chain != null ? chain.otherwise(0) : Expressions.asNumber(0);
    }

    // 상황(situation)에 매핑된 카테고리에 속하면 40점, 아니면 0점.
    private NumberExpression<Integer> situationScoreExpression(QLinku linku, List<Long> mappedCategoryIds) {
        if (mappedCategoryIds == null || mappedCategoryIds.isEmpty()) {
            return Expressions.asNumber(0);
        }
        return new CaseBuilder()
                .when(linku.category.categoryId.in(mappedCategoryIds))
                .then(40)
                .otherwise(0);
    }
}