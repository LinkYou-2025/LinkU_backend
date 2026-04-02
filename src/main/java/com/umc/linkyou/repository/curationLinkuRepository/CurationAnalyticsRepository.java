package com.umc.linkyou.repository.curationLinkuRepository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QRecentViewedLinku;
import com.umc.linkyou.domain.QAiArticle;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.web.dto.curation.CurationAnalyticsDTO.KeywordCountResponse;
import com.umc.linkyou.web.dto.curation.CurationAnalyticsDTO.KeywordLinkResponse;
import com.umc.linkyou.web.dto.curation.CurationAnalyticsDTO.UnreadLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CurationAnalyticsRepository {

    private final JPAQueryFactory queryFactory;

    // [2페이지] 이번 달 많이 본 키워드 랭킹 추출
    public List<KeywordCountResponse> findTopKeywordsByViewedAt(Long userId, LocalDateTime startDate, LocalDateTime endDate, int limit) {
        QRecentViewedLinku rv = QRecentViewedLinku.recentViewedLinku;
        QAiArticle ai = QAiArticle.aiArticle;

        return queryFactory
                .select(Projections.constructor(KeywordCountResponse.class, ai.keyword, rv.count()))
                .from(rv)
                .join(ai).on(rv.linku.eq(ai.linku))
                .where(
                        rv.user.id.eq(userId),
                        rv.viewedAt.goe(startDate),
                        rv.viewedAt.lt(endDate),
                        ai.keyword.isNotNull()
                )
                .groupBy(ai.keyword)
                .orderBy(rv.count().desc())
                .limit(limit)
                .fetch();
    }

    // [2페이지] 특정 키워드를 가진 링크 리스트 조회
    public List<KeywordLinkResponse> findLinksByKeyword(Long userId, String keyword) {
        QUsersLinku ul = QUsersLinku.usersLinku;
        QAiArticle ai = QAiArticle.aiArticle;

        return queryFactory
                .select(Projections.constructor(KeywordLinkResponse.class,
                        ul.userLinkuId, ul.linku.linkuId, ul.linku.title, ul.linku.linku, ul.linku.domain.imageUrl))
                .from(ul)
                .join(ai).on(ul.linku.eq(ai.linku))
                .where(ul.user.id.eq(userId), ai.keyword.eq(keyword))
                .orderBy(ul.createdAt.desc())
                .fetch();
    }

    // [3페이지] 지난달 저장만 하고 한 번도 안 본 링크 추출
    public List<UnreadLinkResponse> findUnreadLinksByCreatedAt(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        QUsersLinku ul = QUsersLinku.usersLinku;
        QRecentViewedLinku rv = QRecentViewedLinku.recentViewedLinku;

        return queryFactory
                .select(Projections.constructor(UnreadLinkResponse.class,
                        ul.userLinkuId,
                        ul.linku.linkuId,
                        ul.linku.title,
                        ul.linku.linku,
                        ul.linku.domain.imageUrl,
                        ul.createdAt.stringValue()
                ))
                .from(ul)
                .where(
                        ul.user.id.eq(userId),
                        ul.createdAt.goe(startDate), // 지난달 1일 이상
                        ul.createdAt.lt(endDate),    // 이번 달 1일 미만
                        // 핵심: 열람 기록(rv) 테이블에 해당 유저+링크 조합이 존재하지 않는(notExists) 것만 필터링!
                        JPAExpressions
                                .selectOne()
                                .from(rv)
                                .where(rv.user.eq(ul.user).and(rv.linku.eq(ul.linku)))
                                .notExists()
                )
                .orderBy(ul.createdAt.asc()) // 저장한 순서대로 정렬
                .fetch();
    }
}