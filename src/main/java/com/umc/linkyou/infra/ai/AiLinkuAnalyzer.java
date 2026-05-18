package com.umc.linkyou.infra.ai;

import com.umc.linkyou.infra.ai.dto.CategoryResultDTO;
import java.util.List;

// 카테고리, 키워드 분류
public interface AiLinkuAnalyzer {
    CategoryResultDTO classifyCategoryByUrl(String url, List<?> categories);
}
