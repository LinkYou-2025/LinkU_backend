package com.umc.linkyou.service.curation.linku.internal;

import java.util.List;

public interface InternalLinkCandidateService {
    List<InternalCandidateDTO> getInternalCandidates(Long userId, Long curationId, int limit);
}