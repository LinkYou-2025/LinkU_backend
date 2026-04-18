package com.umc.linkyou.service.curation.linku;

import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.service.curation.linku.external.ExternalRecommendCacheReader;
import com.umc.linkyou.service.curation.linku.external.ExternalRecommendMaterializer;
import com.umc.linkyou.service.curation.linku.internal.InternalRecommendMaterializer;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurationRecommendBuilderServiceImpl implements CurationRecommendBuilderService {

    private final CurationLinkuRepository curationLinkuRepository;
    private final ExternalRecommendCacheReader externalRecommendCacheReader;
    private final InternalRecommendMaterializer internalRecommendMaterializer;
    private final ExternalRecommendMaterializer externalRecommendMaterializer;

    @Override
    public List<RecommendedLinkResponse> buildRecommendedLinks(Long userId, Long curationId) {
        // 1) 내부 추천 DB 캐시에서 조회
        var internalEntities = curationLinkuRepository
                .findByCuration_CurationIdAndType(curationId, CurationLinkuType.RECOMMENDED);

        if (internalEntities.isEmpty()) {
            internalRecommendMaterializer.generateAndStoreInternalAsync(curationId);
        }

        List<RecommendedLinkResponse> internal = internalEntities.stream()
                .map(e -> RecommendedLinkResponse.builder()
                        .url(e.getUrl())
                        .title(e.getTitle())
                        .imageUrl(e.getImageUrl())
                        .build())
                .toList();

        // 2) 외부 추천 DB 캐시에서 조회
        var external = externalRecommendCacheReader.read(curationId);

        if (external.isEmpty()) {
            externalRecommendMaterializer.generateAndStoreExternalAsync(curationId);
        }

        // 3) 합치기 (내부 4 + 외부 5, 중복 URL 제거)
        var all = new ArrayList<RecommendedLinkResponse>(9);
        all.addAll(internal);
        for (var ex : external) {
            if (all.size() >= 9) break;
            boolean dup = all.stream().anyMatch(i -> i.getUrl().equals(ex.getUrl()));
            if (!dup) all.add(ex);
        }
        return all;
    }
}


