package com.umc.linkyou.service.curation.recommend;

import java.util.List;

import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;

public interface CurationRecommendBuilderService {
    List<RecommendedLinkResponse> buildRecommendedLinks(Long userId, Long curationId);
}
