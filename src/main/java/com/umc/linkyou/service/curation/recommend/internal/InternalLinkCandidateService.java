package com.umc.linkyou.service.curation.recommend.internal;

import java.util.List;

import com.umc.linkyou.domain.mapping.UsersLinku;

public interface InternalLinkCandidateService {
    List<UsersLinku> getInternalCandidates(Long userId, Long curationId, int limit);
}
