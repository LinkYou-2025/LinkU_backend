package com.umc.linkyou.service.aiArticle;

import com.umc.linkyou.web.dto.AiArticleResponsetDTO;

public interface AiArticleService {
    AiArticleResponsetDTO.AiArticleResultDTO saveAiArticle(Long linkuId, Long userId);
    AiArticleResponsetDTO.AiArticleResultDTO showAiArticle(Long linkuId, Long userId);
    AiArticleResponsetDTO.AiArticleResultDTO saveOrGetAiArticle(Long linkuId, Long userId);
}
