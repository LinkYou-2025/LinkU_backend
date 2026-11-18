package com.umc.linkyou.service.Linku;

import java.util.List;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.PageType;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;

// savedUrl은 사용자가 입력한 URL, linkUrl은 linkus에 저장할 대표 URL이다.
record PersistCommand(
        Long userId,
        LinkuRequestDTO.LinkuCreateDTO dto,
        String savedUrl,
        String linkUrl,
        String domainTail,
        List<String> domainTailCandidates,
        Domain preResolvedDomain,
        NewLinkuAiData aiData,
        PageType pageType,
        String userImageUrl) {}
