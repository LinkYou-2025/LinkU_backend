package com.umc.linkyou.converter;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;

public class CurationConverter {

    public static RecommendedLinkResponse toRecommendedLinkResponse(CurationLinku entity) {
        Domain domain = entity.getUsersLinku() != null
                ? entity.getUsersLinku().getLinku().getDomain()
                : null;
        return RecommendedLinkResponse.builder()
                .url(entity.getUrl())
                .title(entity.getTitle())
                .domain(domain != null ? domain.getName() : null)
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .imageUrl(entity.getImageUrl())
                .build();
    }
}
