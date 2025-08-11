package com.umc.linkyou.repository.linkuRepository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.classification.QDomain;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class LinkuRepositoryImpl implements LinkuRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<LinkuSearchSuggestionResponse> findUserSavedSuggestions(Long userId, String keyword) {
        String q = (keyword == null) ? "" : keyword.trim();
        if (q.length() < 2) return List.of();

        QUsersLinku ul = QUsersLinku.usersLinku;
        QLinku l = QLinku.linku1;          // QLinku static 인스턴스
        QDomain d = QDomain.domain;

        // 제목 내 검색어 위치 (앞쪽일수록 값이 작음, 미일치 시 0)
        var pos = Expressions.numberTemplate(Integer.class, "INSTR({0}, {1})", l.title, q);

        return queryFactory
                .select(Projections.constructor(
                        LinkuSearchSuggestionResponse.class,
                        l.title,        // title
                        d.imageUrl,      // domainImageUrl
                        l.linku         // linkUrl
                ))
                .from(ul)
                .join(ul.linku, l)
                .leftJoin(l.domain, d)
                .where(
                        ul.user.id.eq(userId)
                                .and(l.title.containsIgnoreCase(q))
                )
                .orderBy(
                        // 앞쪽 일치 우선 (미일치 pos=0은 가장 뒤로)
                        Expressions.numberTemplate(Integer.class,
                                "CASE WHEN {0}=0 THEN 9999 ELSE {0} END", pos).asc(),
                        l.title.asc()
                )
                .fetch();
    }
}
