package com.umc.linkyou.service.curation.linku;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.infra.parser.LinkToImageService;
import com.umc.linkyou.repository.LogRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepositoryCustom;
import com.umc.linkyou.service.curation.gemini.GeminiExternalSearchService;
import com.umc.linkyou.service.curation.utils.EmotionTagMapper;
import com.umc.linkyou.utils.UrlValidUtils;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalRecommendServiceImpl implements ExternalRecommendService {

    private final InternalLinkCandidateService internalLinkCandidateService;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final DomainRepositoryCustom domainRepository;
    private final LinkToImageService linkToImageService;
    private final GeminiExternalSearchService geminiExternalSearchService;
    private final UserRepository userRepository;
    private final EmotionTagMapper emotionTagMapper;
    private final SituationJobRepository situationJobRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendedLinkResponse> getExternalRecommendations(Long userId, Long curationId, int limit) {

        // 사용자 프로필 로드 (jobName, gender)
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("user not found"));
        String jobName = (user.getJob() != null) ? user.getJob().getName() : null;
        String gender  = (user.getGender() != null) ? user.getGender().name() : null; // MALE / FEMALE

        // 내부 추천으로 최근 URL 확보 (최대 2개)
        List<RecommendedLinkResponse> internalLinks = internalLinkCandidateService.getInternalCandidates(userId, curationId, 2);
        int externalLimit = 1;

        // 최근 URL과 사용자 상위 태그 확보
        List<String> recentUrls = internalLinks.stream()
                .map(RecommendedLinkResponse::getUrl)
                .toList();

        List<String> tagNames = keywordMonthlyCountRepository.findTop3ByUser_IdOrderByCountDesc(userId)
                .stream()
                .map(kmc -> kmc.getType() == KeywordType.EMOTION
                        ? emotionTagMapper.getEmotionName(kmc.getRefId())
                        : situationJobRepository.findById(kmc.getRefId())
                                .map(sj -> sj.getSituation().getName())
                                .orElse(""))
                .filter(name -> !name.isBlank())
                .toList();

        // Gemini 기반 외부 추천 받기
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
            log.warn("[Gemini] 외부 추천 실패: {}", e.getMessage());
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


