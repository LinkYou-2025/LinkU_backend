package com.umc.linkyou.infra.ai;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import java.util.List;
import java.util.Optional;

public interface AiLinkuAnalyzer {
    // 카테고리, 키워드 분류
    Optional<LinkuResultDTO> analyzeByUrl(String url, List<Category> categories);
}
