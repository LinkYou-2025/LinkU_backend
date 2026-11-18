package com.umc.linkyou.service.keyword;

import java.time.YearMonth;
import java.util.List;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.web.dto.keyword.JobKeywordRankResponse;
import com.umc.linkyou.web.dto.keyword.KeywordLinkuItemDTO;

public interface KeywordService {
    void saveKeywords(Linku linku, String rawKeywords);

    List<JobKeywordRankResponse> getJobTopKeywords(Long userId, YearMonth month, int limit);

    List<KeywordLinkuItemDTO> getLinkusByKeyword(Long userId, String keyword);
}
