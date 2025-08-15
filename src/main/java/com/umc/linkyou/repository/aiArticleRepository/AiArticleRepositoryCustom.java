package com.umc.linkyou.repository.aiArticleRepository;

import java.util.Collection;
import java.util.Map;

public interface AiArticleRepositoryCustom {
    boolean existsAiArticleByLinkuId(Long linkuId);

    // 다수 linkuId 존재 여부 조회 (최적화용)
    Map<Long, Boolean> existsAiArticleByLinkuIds(Collection<Long> linkuIds);
}
