package com.umc.linkyou.service.keyword;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.web.dto.keyword.JobKeywordRankResponse;

import java.util.List;

public interface KeywordService {
    void saveKeywords(Linku linku, String rawKeywords);

    List<JobKeywordRankResponse> getJobTopKeywords(Long userId, String month, int limit);
}
