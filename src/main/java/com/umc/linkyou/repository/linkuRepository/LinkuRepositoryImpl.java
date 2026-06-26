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
        QUsersLinku ul = QUsersLinku.usersLinku;
        QLinku l = QLinku.linku;
        QDomain d = QDomain.domain;

        return queryFactory
                .select(Projections.constructor(
                        LinkuSearchSuggestionResponse.class,
                        l.linkuId,
                        l.title,
                        d.imageUrl,
                        l.linkuUrl
                ))
                .from(ul)
                .join(ul.linku, l)
                .leftJoin(l.domain, d)
                .where(
                        ul.user.id.eq(userId),
                        l.title.startsWith(keyword)
                )
                .orderBy(l.title.asc())
                .limit(10)
                .fetch();
    }
    @Override
    public Optional<Linku> findByLinku(String normalizedLink) {
        QLinku l = QLinku.linku;

        // AiArticle만 fetch join (이후 바로 null 체크 및 세팅하기 때문)
        Linku result = queryFactory
                .selectFrom(l)
                .leftJoin(l.aiArticle).fetchJoin()
                .where(l.linkuUrl.eq(normalizedLink))
                .fetchFirst();

        return Optional.ofNullable(result);
    }

}
