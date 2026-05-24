package com.umc.linkyou.service.curation.linku.internal;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.log.KeywordMonthlyCount;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.repository.mapping.SituationJobRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.service.Linku.SituationCategoryService;
import com.umc.linkyou.utils.EmotionSimilarityUtil;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InternalLinkCandidateServiceImpl implements InternalLinkCandidateService {

    private final UsersLinkuRepository usersLinkuRepository;
    private final CurationRepository curationRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final SituationJobRepository situationJobRepository;
    private final SituationCategoryService situationCategoryService;

    @Override
    public List<RecommendedLinkResponse> getInternalCandidates(Long userId, Long curationId, int limit) {
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("큐레이션 없음"));

        // 큐레이션 생성 월 계산
        YearMonth ym = YearMonth.parse(curation.getMonth());
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd   = ym.plusMonths(1).atDay(1).atStartOfDay();

        // 유저가 해당 월에 저장한 링크 조회 (없으면 빈 리스트 반환)
        List<UsersLinku> candidates = usersLinkuRepository
                .findAllByUserIdAndCreatedAtBetween(userId, monthStart, monthEnd);
        if (candidates.isEmpty()) {
            return List.of();
        }

        // 해당 월 상위 감정 ID 조회
        Long topEmotionId = keywordMonthlyCountRepository
                .findTopByUserIdAndBaseMonthAndType(userId, curation.getMonth(), KeywordType.EMOTION, PageRequest.of(0, 1))
                .stream().findFirst()
                .map(KeywordMonthlyCount::getRefId)
                .orElse(null);

        // 해당 월 상위 상황 -> 추천 카테고리 ID 목록 조회
        List<Long> mappedCategoryIds;
        var topSituationOpt = keywordMonthlyCountRepository
                .findTopByUserIdAndBaseMonthAndType(userId, curation.getMonth(), KeywordType.SITUATION, PageRequest.of(0, 1))
                .stream().findFirst();

        if (topSituationOpt.isPresent()) {
            mappedCategoryIds = situationJobRepository.findById(topSituationOpt.get().getRefId())
                    .map(sj -> situationCategoryService.getCategoryIdsBySituation(sj.getSituation().getId()))
                    .orElse(List.of());
        } else {
            mappedCategoryIds = List.of();
        }

        // 스코어링: 감정(최대 60) + 상황(최대 40)
        List<Pair<UsersLinku, Integer>> scoredLinks = new ArrayList<>();

        for (UsersLinku link : candidates) {
            // 감정 점수
            int emotionScore = EmotionSimilarityUtil.getSimilarityScore(
                    topEmotionId,
                    link.getEmotion().getEmotionId()
            );

            // 상황 점수
            Long aiCategoryId = (link.getLinku().getAiArticle() != null)
                    ? link.getLinku().getAiArticle().getAiCategoryId()
                    : null;
            int situationScore = (aiCategoryId != null && mappedCategoryIds.contains(aiCategoryId)) ? 40 : 0;

            scoredLinks.add(Pair.of(link, emotionScore + situationScore));
        }

        // 점수 내림차순 정렬 후 limit 개수 추출 (동점 시 최신순)
        List<UsersLinku> finalLinks = scoredLinks.stream()
                .sorted((a, b) -> {
                    int scoreCmp = Integer.compare(b.getRight(), a.getRight());
                    if (scoreCmp != 0) return scoreCmp;
                    return b.getLeft().getCreatedAt().compareTo(a.getLeft().getCreatedAt());
                })
                .map(Pair::getLeft)
                .limit(limit)
                .toList();

        // DTO 변환
        return finalLinks.stream()
                .map(link -> RecommendedLinkResponse.builder()
                        .userLinkuId(link.getUserLinkuId())
                        .title(link.getLinku().getTitle())
                        .url(link.getLinku().getLinku())
                        .domain(link.getLinku().getDomain().getName())
                        .domainImageUrl(link.getLinku().getDomain().getImageUrl())
                        .build()
                )
                .toList();
    }
}
