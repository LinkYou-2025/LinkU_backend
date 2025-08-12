package com.umc.linkyou.service.curation.linku;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.TitleImgParser.LinkToImageService;
import com.umc.linkyou.repository.classification.DomainRepository;
import com.umc.linkyou.repository.curationLinkuRepository.CurationLinkuRepository;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.umc.linkyou.service.Linku.LinkuServiceImpl.extractDomainTail;

@Service
@RequiredArgsConstructor
public class ExternalRecommendCacheReader {

    private final CurationLinkuRepository curationLinkuRepository;
    private final DomainRepository domainRepository;
    private final LinkToImageService linkToImageService;

    /** DB 캐시(EXTERNAL) → 도메인/이미지 보강 후 DTO */
    @Transactional(readOnly = true)
    public List<RecommendedLinkResponse> read(Long curationId) {
        var entities = curationLinkuRepository
                .findByCuration_CurationIdAndType(curationId, CurationLinkuType.EXTERNAL);

        return entities.stream().map(e -> {
            String url = e.getUrl();
            String title = e.getTitle();

            String domainTail = extractDomainTail(url);
            var domain = domainRepository.findByDomainTail(domainTail)
                    .orElse(Domain.builder().name("unknown").imageUrl(null).build());
            String imageUrl = linkToImageService.getRelatedImageFromUrl(url);

            return RecommendedLinkResponse.builder()
                    .url(url)
                    .title(title)
                    .domain(domain.getName())
                    .domainImageUrl(domain.getImageUrl())
                    .imageUrl(imageUrl)
                    .build();
        }).toList();
    }
}
