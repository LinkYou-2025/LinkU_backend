package com.umc.linkyou.service.curation.recommend.internal;

import com.umc.linkyou.domain.mapping.UsersLinku;
import java.util.List;

public interface InternalLinkCandidateService {
    List<UsersLinku> getInternalCandidates(Long userId, Long curationId, int limit);
}
