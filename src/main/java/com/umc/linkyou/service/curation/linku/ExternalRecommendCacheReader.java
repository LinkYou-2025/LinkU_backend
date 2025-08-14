package com.umc.linkyou.service.curation.linku;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.TitleImgParser.LinkToImageService;
import com.umc.linkyou.repository.classification.DomainRepository;
import com.umc.linkyou.repository.curationLinkuRepository.CurationLinkuRepository;
import com.umc.linkyou.utils.UrlValidUtils;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExternalRecommendCacheReader {

    private final CurationLinkuRepository curationLinkuRepository;
    private final DomainRepository domainRepository;

    private static final Domain UNKNOWN_DOMAIN =
            Domain.builder().name("unknown").imageUrl(null).build();

    /** DB 캐시(EXTERNAL) → 도메인 보강 후 DTO (이미지 URL은 DB 저장값 그대로 사용) */
    @Transactional(readOnly = true)
    public List<RecommendedLinkResponse> read(Long curationId) {
        var entities = curationLinkuRepository
                .findByCuration_CurationIdAndType(curationId, CurationLinkuType.EXTERNAL);

        // 1) URL → domainTail 추출
        var items = entities.stream()
                .map(e -> new Temp(
                        e.getUrl(),
                        e.getTitle(),
                        e.getImageUrl(),                      // ← DB에 저장된 이미지 그대로 사용
                        UrlValidUtils.extractDomainTail(e.getUrl())))
                .toList();

        // 2) 고유 domainTail 수집 후 일괄 조회
        var tails = items.stream()
                .map(Temp::domainTail)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .toList();

        Map<String, Domain> domainMap = domainRepository.findByDomainTailIn(tails).stream()
                .collect(Collectors.toMap(Domain::getDomainTail, Function.identity()));

        // 3) DTO 매핑
        return items.stream()
                .map(t -> {
                    var domain = t.domainTail()!=null
                            ? domainMap.getOrDefault(t.domainTail(), UNKNOWN_DOMAIN)
                            : UNKNOWN_DOMAIN;

                    return RecommendedLinkResponse.builder()
                            .url(t.url())
                            .title(t.title())
                            .domain(domain.getName())
                            .domainImageUrl(domain.getImageUrl())
                            .imageUrl(t.imageUrl()) // ← 크롤링 없이 DB 값
                            .build();
                })
                .toList();
    }

    private record Temp(String url, String title, String imageUrl, String domainTail) {}
}
