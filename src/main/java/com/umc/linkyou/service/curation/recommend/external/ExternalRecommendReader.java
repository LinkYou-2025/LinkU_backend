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
import java.util.Objects;
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

        // domainTailCandidates: [정확한 호스트, (있다면) registry-suffix apex 도메인] 순서.
        // someuser.tistory.com처럼 정확히 일치하는 domains 행이 없는 서브도메인도
        // apex(tistory.com) 행으로 폴백해 브랜딩 정보를 붙일 수 있게 한다.
        List<ExternalItem> items = entities.stream()
                .map(e -> new ExternalItem(
                        e.getUrl(),
                        e.getTitle(),
                        e.getImageUrl(),
                        UrlValidUtils.extractDomainTailCandidates(e.getUrl())))
                .toList();

        List<String> tails = items.stream()
                .flatMap(item -> item.domainTailCandidates().stream())
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
                    Domain domain = item.domainTailCandidates().stream()
                            .map(domainMap::get)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);
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

    private record ExternalItem(String url, String title, String imageUrl, List<String> domainTailCandidates) {}
}
