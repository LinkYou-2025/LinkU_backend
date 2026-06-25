package com.umc.linkyou.infra.ai;

import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;

public interface AiArticleAnalyzer {
    AiArticleResultDTO analyzeByUrl(String url);
}
