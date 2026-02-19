package com.umc.linkyou.service.curation.linku;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.titleImgParser.LinkToImageService;
import com.umc.linkyou.repository.LogRepository.CurationTopLogRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepositoryCustom;
import com.umc.linkyou.service.curation.gpt.GptService;
import com.umc.linkyou.domain.log.CurationTopLog;
// import com.umc.linkyou.service.curation.perplexity.PerplexityExternalSearchService;
import com.umc.linkyou.service.curation.gemini.GeminiExternalSearchService;
import com.umc.linkyou.utils.UrlValidUtils;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ExternalRecommendServiceImpl implements ExternalRecommendService {

    private final InternalLinkCandidateService internalLinkCandidateService;
    private final CurationTopLogRepository curationTopLogRepository;
    private final GptService gptService;
    private final DomainRepositoryCustom domainRepository;
    private final LinkToImageService linkToImageService;
    // private final PerplexityExternalSearchService perplexityExternalSearchService; // Perplexity 사용
    private final GeminiExternalSearchService geminiExternalSearchService; // Gemini 사용
    private final UserRepository userRepository;

    @Override
    public List<RecommendedLinkResponse> getExternalRecommendations(Long userId, Long curationId, int limit) {

        // 사용자 프로필 로드 (jobName, gender)
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        String jobName = (user.getJob() != null) ? user.getJob().getName() : null;
        String gender  = (user.getGender() != null) ? user.getGender().name() : null; // MALE / FEMALE

        // 내부 추천으로 최근 URL 확보 (최대 4개)
        List<RecommendedLinkResponse> internalLinks = internalLinkCandidateService.getInternalCandidates(userId, curationId, 4);
        int externalLimit = 9 - internalLinks.size();

        // 최근 URL과 사용자 상위 태그 확보
        List<String> recentUrls = internalLinks.stream()
                .map(RecommendedLinkResponse::getUrl)
                .toList();

        List<String> tagNames = curationTopLogRepository.findTopTagsByUserId(userId, 3)
                .stream()
                .map(CurationTopLog::getTagName)
                .toList();

        // Perplexity 기반 외부 추천 받기
        List<RecommendedLinkResponse> external;
        try {
            external = geminiExternalSearchService.searchExternalLinks(
                    recentUrls,
                    tagNames,
                    externalLimit,
                    jobName,
                    gender
            );
        } catch (Exception e) {
            // 🔴 어떤 예외가 와도 외부는 포기하고 빈 리스트로 폴백
            System.err.println("[Perplexity] 외부 추천 실패: " + e.getMessage());
            external = List.of();
        }

        // 도메인/이미지 보강
        return external.stream().map(item -> {
            String url = item.getUrl();
            String domainTail = UrlValidUtils.extractDomainTail(url);
            var domain = domainRepository.findByDomainTail(domainTail)
                    .orElse(Domain.builder().name("unknown").imageUrl(null).build());
            String imageUrl = linkToImageService.getRelatedImageFromUrl(url);

            return item.toBuilder()
                    .domain(domain.getName())
                    .domainImageUrl(domain.getImageUrl())
                    .imageUrl(imageUrl)
                    .build();
        }).toList();
    }
}


