package com.umc.linkyou.repository.aiArticleRepository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.repository.linkuRepository.LinkuRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

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
        return fetchOne != null;
    }

    @Override
    public Map<Long, Boolean> existsAiArticleByLinkuIds(Collection<Long> linkuIds) {
        if (linkuIds == null || linkuIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // AI 아티클 존재하는 linkuId 리스트 조회
        List<Long> existingIds = queryFactory
                .select(linku.linkuId)
                .from(linku)
                .where(linku.linkuId.in(linkuIds)
                        .and(linku.title.isNotNull())
                        .and(linku.title.ne("")))
                .fetch();

        // 결과를 Map<Long, Boolean> 형식으로 반환 (존재하면 true)
        Map<Long, Boolean> result = new HashMap<>();
        for (Long id : linkuIds) {
            result.put(id, existingIds.contains(id));
        }
        return result;
    }
}
