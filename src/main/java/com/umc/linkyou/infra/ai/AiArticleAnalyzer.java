package com.umc.linkyou.infra.ai;

import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import java.util.List;

public interface AiArticleAnalyzer {
    // 제목, 요약, 감정, 상황
    AiArticleResultDTO analyzeByUrl(String url, List<Situation> situations, List<Emotion> emotions);
}
