package com.umc.linkyou.service.curation.linku;

import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import java.util.List;

public interface CurationRecommendBuilderService {
    List<RecommendedLinkResponse> buildRecommendedLinks(Long userId, Long curationId);
}