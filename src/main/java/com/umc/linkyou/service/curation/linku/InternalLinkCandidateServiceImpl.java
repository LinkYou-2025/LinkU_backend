package com.umc.linkyou.service.curation.linku;


import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.TitleImgParser.LinkToImageService;
import com.umc.linkyou.repository.CurationRepository;
import com.umc.linkyou.repository.LogRepository.CurationTopLogRepository;
import com.umc.linkyou.repository.mapping.UsersLinkuRepository;
import com.umc.linkyou.service.curation.utils.EmotionSimilarityTable;
import com.umc.linkyou.service.curation.utils.EmotionTagMapper;
import com.umc.linkyou.service.curation.utils.TextEmbeddingUtil;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InternalLinkCandidateServiceImpl implements InternalLinkCandidateService {

    private final UsersLinkuRepository usersLinkuRepository;
    private final CurationRepository curationRepository;
    private final CurationTopLogRepository curationTopLogRepository;
    private final LinkToImageService linkToImageService;
    private final EmotionTagMapper emotionTagMapper;

    @Override
    public List<RecommendedLinkResponse> getInternalCandidates(Long userId, Long curationId, int limit) {
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("큐레이션 없음"));

        // 1. 큐레이션 생성 월 계산
        LocalDateTime monthStart = curation.getCreatedAt().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        // 2. 유저가 해당 월에 저장한 링크 조회
        List<UsersLinku> candidates = usersLinkuRepository
                .findAllByUserIdAndCreatedAtBetween(userId, monthStart, monthEnd);

        // 상위 감정명 추출 (예: 슬픔, 분노, 짜증)
        List<String> topEmotionNames = curationTopLogRepository.findTop3EmotionLogsByCurationId(curationId).stream()
                .map(log -> emotionTagMapper.getEmotionName(log.getRefId()))
                .toList();

        // TF-IDF 벡터화 대상 (title 기준)
        List<String> corpus = candidates.stream()
                .map(link -> link.getLinku().getTitle() != null ? link.getLinku().getTitle() : "")
                .collect(Collectors.toList());

        // 큐레이션 감정명을 기준 텍스트로 추가 (예: "슬픔 분노 짜증")
        String curationEmotionText = String.join(" ", topEmotionNames);
        corpus.add(curationEmotionText);

        // TF-IDF 벡터 계산
        List<Map<String, Double>> vectors = TextEmbeddingUtil.computeTfidf(corpus);
        Map<String, Double> queryVec = vectors.get(vectors.size() - 1);

        // 점수 계산
        List<Pair<UsersLinku, Double>> scoredLinks = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            UsersLinku link = candidates.get(i);

            // 1. TF-IDF 유사도
            double tfidfSim = TextEmbeddingUtil.cosineSimilarity(vectors.get(i), queryVec);

            // 2. 감정 유사도 계산 (단, "평온"이면 무조건 0점)
            String linkEmotion = link.getEmotion().getName();
            double emotionSim = 0.0;

            if (!"평온".equals(linkEmotion)) {
                emotionSim = topEmotionNames.stream()
                        .mapToDouble(top -> EmotionSimilarityTable.getSimilarity(top, linkEmotion))
                        .max().orElse(0.0);
            }

            // 3. 최종 점수
            double finalScore = (emotionSim * 0.7) + (tfidfSim * 0.3);
            scoredLinks.add(Pair.of(link, finalScore));
        }

        // 점수 내림차순 정렬 후 limit 개수 추출
        List<UsersLinku> finalLinks = scoredLinks.stream()
                .sorted((a, b) -> Double.compare(b.getRight(), a.getRight()))
                .map(Pair::getLeft)
                .limit(limit)
                .toList();

        // 6. DTO 변환
        return finalLinks.stream()
                .map(link -> {
                    String url = link.getLinku().getLinku();
                    String imageUrl = linkToImageService.getRelatedImageFromUrl(url);
                    List<String> categories = List.of(link.getLinku().getCategory().getCategoryName());

                    return RecommendedLinkResponse.builder()
                            .userLinkuId(link.getUserLinkuId())
                            .title(link.getLinku().getTitle())
                            .url(url)
                            .domain(link.getLinku().getDomain().getName())
                            .domainImageUrl(link.getLinku().getDomain().getImageUrl())
                            .imageUrl(imageUrl)
                            .categories(categories)
                            .build();
                })
                .toList();
    }
    @Override
    public List<RecommendedLinkResponse> getTop2SimilarInternalLinks(Long userId, Long curationId) {
        return getInternalCandidates(userId, curationId, 2);
    }
}
