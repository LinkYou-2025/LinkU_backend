package com.umc.linkyou.service.curation.linku.internal;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalRecommendWorker {

    private final CurationRepository curationRepository;
    private final CurationLinkuRepository curationLinkuRepository;
    private final InternalLinkCandidateService internalLinkCandidateService;
    private final LinkToImageService linkToImageService;

    @Transactional
    public void generateAndStoreInternal(Long curationId) {
        log.info("[INT] start materialize curationId={}", curationId);

        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("curation not found"));
        Long userId = curation.getUser().getId();

        List<RecommendedLinkResponse> candidates = internalLinkCandidateService.getInternalCandidates(userId, curationId, 4);

        curationLinkuRepository.deleteAllByCurationIdAndType(curationId, CurationLinkuType.RECOMMENDED);

        for (var item : candidates) {
            if (item.getUrl() == null || item.getUrl().isBlank()) continue;

            String imageUrl = fetchImageUrlFast(item.getUrl());

            curationLinkuRepository.save(
                    CurationLinku.builder()
                            .curation(curation)
                            .type(CurationLinkuType.RECOMMENDED)
                            .url(item.getUrl())
                            .title(item.getTitle())
                            .imageUrl(imageUrl)
                            .build()
            );
        }

        log.info("[INT] saved rows={}", candidates.size());
    }

    private String fetchImageUrlFast(String url) {
        try {
            return linkToImageService.getRelatedImageFromUrl(url);
        } catch (Exception e) {
            return null;
        }
    }
}
