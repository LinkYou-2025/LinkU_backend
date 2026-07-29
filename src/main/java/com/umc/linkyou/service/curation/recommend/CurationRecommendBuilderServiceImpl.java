package com.umc.linkyou.service.curation.recommend;

import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.CurationConverter;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.service.curation.recommend.external.ExternalRecommendMaterializer;
import com.umc.linkyou.service.curation.recommend.external.ExternalRecommendReader;
import com.umc.linkyou.service.curation.recommend.internal.InternalRecommendMaterializer;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CurationRecommendBuilderServiceImpl implements CurationRecommendBuilderService {

    private static final int INTERNAL_LIMIT = 4;
    private static final int EXTERNAL_LIMIT = 5;

    private final CurationRepository curationRepository;
    private final CurationLinkuRepository curationLinkuRepository;
    private final ExternalRecommendReader externalRecommendReader;
    private final InternalRecommendMaterializer internalRecommendMaterializer;
    private final ExternalRecommendMaterializer externalRecommendMaterializer;

    @Override
    public List<RecommendedLinkResponse> buildRecommendedLinks(Long userId, Long curationId) {
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new GeneralException(CurationErrorStatus._CURATION_NOT_FOUND));

        if (!curation.getUser().getId().equals(userId)) {
            throw new GeneralException(CurationErrorStatus._CURATION_FORBIDDEN);
        }

        List<RecommendedLinkResponse> internal = curationLinkuRepository
                .findWithDomainByCurationIdAndType(curationId, CurationLinkuType.INTERNAL)
                .stream()
                .map(CurationConverter::toRecommendedLinkResponse)
                .limit(INTERNAL_LIMIT)
                .toList();

        if (internal.isEmpty()) {
            internalRecommendMaterializer.generateInternalAsync(curationId);
        }

        List<RecommendedLinkResponse> external = externalRecommendReader.read(curationId);

        if (external.isEmpty()) {
            externalRecommendMaterializer.generateExternalAsync(curationId);
        }

        List<RecommendedLinkResponse> result = new ArrayList<>(INTERNAL_LIMIT + EXTERNAL_LIMIT);
        result.addAll(internal);

        Set<String> seenUrls = internal.stream()
                .map(RecommendedLinkResponse::getUrl)
                .collect(Collectors.toCollection(HashSet::new));

        for (RecommendedLinkResponse ex : external) {
            if (result.size() >= INTERNAL_LIMIT + EXTERNAL_LIMIT) break;
            if (seenUrls.add(ex.getUrl())) result.add(ex);
        }

        return result;
    }
}
