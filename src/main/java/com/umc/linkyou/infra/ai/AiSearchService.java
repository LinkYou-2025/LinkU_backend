package com.umc.linkyou.infra.ai;

import com.umc.linkyou.infra.ai.dto.ExternalLinkDTO;
import java.util.List;

public interface AiSearchService {
    List<ExternalLinkDTO> searchExternalLinks(List<String> tagNames, int limit, String jobName, String gender);
}
