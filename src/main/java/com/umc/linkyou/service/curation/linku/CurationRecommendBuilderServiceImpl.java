package com.umc.linkyou.service.curation.linku;

import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CurationRecommendBuilderServiceImpl implements CurationRecommendBuilderService {

    private final InternalLinkCandidateService internalLinkCandidateService;
    private final ExternalRecommendCacheReader externalRecommendCacheReader;
    private final ExternalRecommendMaterializer externalRecommendMaterializer; // (선택) 캐시 없을 때 트리거용

    @Override
    public List<RecommendedLinkResponse> buildRecommendedLinks(Long userId, Long curationId) {
        // 1) 내부 4개
        var internal = internalLinkCandidateService.getInternalCandidates(userId, curationId, 4);

        // 2) 외부(캐시에서 즉시)
        var external = externalRecommendCacheReader.read(curationId);

        // (선택) 캐시 비었으면 비동기 생성 트리거 → 다음 진입부터 노출
        if (external.isEmpty()) {
            externalRecommendMaterializer.generateAndStoreExternalAsync(curationId);
        }

        // 3) 합치기(최대 9개, 중복 URL 제거)
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


