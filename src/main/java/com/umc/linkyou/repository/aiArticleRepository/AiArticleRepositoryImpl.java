package com.umc.linkyou.repository.aiArticleRepository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.repository.linkuRepository.LinkuRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AiArticleRepositoryImpl implements AiArticleRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QLinku linku = QLinku.linku1;

    @Override
    public boolean existsAiArticleByLinkuId(Long linkuId) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(linku)
                .where(linku.linkuId.eq(linkuId)
                        .and(linku.title.isNotNull())
                        .and(linku.title.ne("")))
                .fetchFirst();

        return fetchOne != null; // 결과가 있으면 true
    }
}

