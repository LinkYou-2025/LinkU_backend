package com.umc.linkyou.service.keyword;

import com.umc.linkyou.web.dto.keyword.KeywordRankResponse;

import java.util.List;

public interface KeywordService {
    List<KeywordRankResponse> getMyTop3Keywords(Long userId, String month);

    List<KeywordRankResponse> getJobTop15Keywords(Long userId);
}
