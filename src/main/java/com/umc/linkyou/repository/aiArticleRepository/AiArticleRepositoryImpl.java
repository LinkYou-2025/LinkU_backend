package com.umc.linkyou.repository.aiArticleRepository;

import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QAiArticle;
import com.umc.linkyou.domain.QLinku;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AiArticleRepositoryImpl implements AiArticleRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QAiArticle aiArticle = QAiArticle.aiArticle;
    private final QLinku linku = QLinku.linku1;

    @Override
    public boolean existsAiArticleByLinkuId(Long linkuId) {
        Integer count = queryFactory
                .selectOne()
                .from(aiArticle)
                .where(aiArticle.linku.linkuId.eq(linkuId)
                        .and(aiArticle.title.isNotNull())
                        .and(aiArticle.title.isNotEmpty()))
                .fetchFirst();
        return count != null;
    }

    @Override
    public Map<Long, Boolean> existsAiArticleByLinkuIds(Collection<Long> linkuIds) {
        List<Tuple> results = queryFactory
                .select(aiArticle.linku.linkuId, aiArticle.title)
                .from(aiArticle)
                .where(aiArticle.linku.linkuId.in(linkuIds))
                .fetch();

        return results.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(aiArticle.linku.linkuId),
                        tuple -> {
                            String title = tuple.get(aiArticle.title);
                            return title != null && !title.isBlank();
                        },
                        (a, b) -> a
                ));
    }
}
