package com.umc.linkyou.infra.ai;

import com.umc.linkyou.infra.ai.dto.ExternalLinkResultDTO;
import com.umc.linkyou.infra.ai.dto.ExternalSearchRequest;
import java.util.List;

public interface AiSearchService {
    List<ExternalLinkResultDTO> searchExternalLinks(ExternalSearchRequest request);
}
