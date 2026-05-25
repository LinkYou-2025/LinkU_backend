package com.umc.linkyou.service.keyword;

import com.umc.linkyou.web.dto.keyword.KeywordRankResponse;

import java.util.List;

public interface KeywordService {
    List<KeywordRankResponse> getMyTopKeywords(Long userId, String month, int limit);

    List<KeywordRankResponse> getJobTopKeywords(Long userId, int limit);
}
