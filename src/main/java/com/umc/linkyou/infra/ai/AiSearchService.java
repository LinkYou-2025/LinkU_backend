package com.umc.linkyou.infra.ai;

import java.util.List;

import com.umc.linkyou.infra.ai.dto.ExternalLinkResultDTO;
import com.umc.linkyou.infra.ai.dto.ExternalSearchRequest;

public interface AiSearchService {
    List<ExternalLinkResultDTO> searchExternalLinks(ExternalSearchRequest request);
}
