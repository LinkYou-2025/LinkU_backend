package com.umc.linkyou.service.curation.recommend.external;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepositoryCustom;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import com.umc.linkyou.utils.UrlValidUtils;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ExternalRecommendReader {

    private final CurationLinkuRepository curationLinkuRepository;
    private final DomainRepositoryCustom domainRepository;

    @Transactional(readOnly = true)
    public List<RecommendedLinkResponse> read(Long curationId) {
        List<CurationLinku> entities = curationLinkuRepository
                .findByCurationIdAndType(curationId, CurationLinkuType.EXTERNAL);

        List<ExternalItem> items = entities.stream()
                .map(e -> new ExternalItem(
                        e.getUrl(),
                        e.getTitle(),
                        e.getImageUrl(),
                        UrlValidUtils.extractDomainTail(e.getUrl())))
                .toList();

        List<String> tails = items.stream()
                .map(ExternalItem::domainTail)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();

        if (tails.isEmpty()) {
            return items.stream()
                    .map(item -> RecommendedLinkResponse.builder()
                            .url(item.url())
                            .title(item.title())
                            .imageUrl(item.imageUrl())
                            .build())
                    .toList();
        }

        Map<String, Domain> domainMap = domainRepository.findByDomainTailIn(tails).stream()
                .collect(Collectors.toMap(Domain::getDomainTail, Function.identity()));

        return items.stream()
                .map(item -> {
                    Domain domain = item.domainTail() != null ? domainMap.get(item.domainTail()) : null;
                    return RecommendedLinkResponse.builder()
                            .url(item.url())
                            .title(item.title())
                            .domain(domain != null ? domain.getName() : null)
                            .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                            .imageUrl(item.imageUrl())
                            .build();
                })
                .toList();
    }

    private record ExternalItem(String url, String title, String imageUrl, String domainTail) {}
}
