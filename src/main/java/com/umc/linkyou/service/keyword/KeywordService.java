package com.umc.linkyou.service.keyword;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.web.dto.keyword.JobKeywordRankResponse;

import java.time.YearMonth;
import java.util.List;

public interface KeywordService {
    void saveKeywords(Linku linku, String rawKeywords);

    List<JobKeywordRankResponse> getJobTopKeywords(Long userId, YearMonth month, int limit);
}
