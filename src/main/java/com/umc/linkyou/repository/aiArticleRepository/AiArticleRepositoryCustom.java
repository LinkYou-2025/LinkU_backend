package com.umc.linkyou.repository.aiArticleRepository;

import java.util.Collection;
import java.util.Map;

public interface AiArticleRepositoryCustom {
    boolean existsAiArticleByLinkuId(Long linkuId);
    Map<Long, Boolean> existsAiArticleByLinkuIds(Collection<Long> linkuIds);
}
