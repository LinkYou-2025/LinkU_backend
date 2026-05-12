package com.umc.linkyou.repository.linkuRepository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.classification.QDomain;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import com.umc.linkyou.repository.linkuRepository.LinkuRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LinkuRepositoryImpl implements LinkuRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<LinkuSearchSuggestionResponse> findUserSavedSuggestions(Long userId, String keyword) {
        String q = keyword == null ? "" : keyword.trim();
        if (q.length() < 2) return List.of();

        QUsersLinku ul = QUsersLinku.usersLinku;
        QLinku l = QLinku.linku1;
        QDomain d = QDomain.domain;

        // 대소문자 무시 검색 위치 계산
        var pos = Expressions.numberTemplate(
                Integer.class,
                "LOCATE(LOWER({0}), LOWER({1}))",
                q, (Object) l.title // Ambiguous call 방지를 위한 캐스팅
        );

        return queryFactory
                .select(Projections.constructor(
                        LinkuSearchSuggestionResponse.class,
                        l.linkuId,
                        l.title,
                        d.imageUrl,
                        l.linku
                ))
                .from(ul)
                .join(ul.linku, l)
                .leftJoin(l.domain, d)
                .where(
                        ul.user.id.eq(userId),
                        l.title.containsIgnoreCase(q) // QueryDSL이 내부적으로 lower() 처리해줌
                )
                .orderBy(
                        Expressions.numberTemplate(
                                Integer.class,
                                "CASE WHEN {0} = 0 THEN 9999 ELSE {0} END",
                                (Object) pos // Ambiguous call 방지를 위한 캐스팅
                        ).asc(),
                        l.title.asc()
                )
                .limit(10) // 누락되었던 성능 최적화 포인트 추가
                .fetch();
    }
    @Override
    public Optional<Linku> findByLinku(String normalizedLink) {
        QLinku l = QLinku.linku1;

        // AiArticle만 fetch join (이후 바로 null 체크 및 세팅하기 때문)
        Linku result = queryFactory
                .selectFrom(l)
                .leftJoin(l.aiArticle).fetchJoin()
                .where(l.linku.eq(normalizedLink))
                .fetchOne();

        return Optional.ofNullable(result);
    }

}
