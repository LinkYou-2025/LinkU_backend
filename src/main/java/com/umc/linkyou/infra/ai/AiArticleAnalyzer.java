package com.umc.linkyou.infra.ai;

import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import java.io.IOException;
import java.util.List;

// 제목, 요약, 감정, 상황, 카테고리 요약
public interface AiArticleAnalyzer {
    AiArticleResultDTO getFullAnalysis(String url, List<?> situations, List<?> emotions, List<?> categories) throws IOException;
}
