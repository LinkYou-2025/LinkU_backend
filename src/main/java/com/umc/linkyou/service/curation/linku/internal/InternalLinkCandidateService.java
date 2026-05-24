package com.umc.linkyou.service.curation.linku.internal;

import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import java.util.List;

public interface InternalLinkCandidateService {
    List<RecommendedLinkResponse> getInternalCandidates(Long userId, Long curationId, int limit);
}