package com.umc.linkyou.service.curation.recommend.internal;

import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.log.KeywordMonthlyCount;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.repository.mapping.SituationCategoryRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.utils.EmotionSimilarityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InternalLinkCandidateServiceImpl implements InternalLinkCandidateService {

    private final UsersLinkuRepository usersLinkuRepository;
    private final CurationRepository curationRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final SituationCategoryRepository situationCategoryRepository;

    @Override
    public List<UsersLinku> getInternalCandidates(Long userId, Long curationId, int limit) {
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new GeneralException(CurationErrorStatus._CURATION_NOT_FOUND));

        String baseMonth = curation.getBaseMonth();

        // 큐레이션 생성 월 계산
        YearMonth ym = YearMonth.parse(baseMonth);
        LocalDateTime monthStart = ym.atDay(1).atStartOfDay();
        LocalDateTime monthEnd   = ym.plusMonths(1).atDay(1).atStartOfDay();

        // 유저가 해당 월에 저장한 링크 조회 (없으면 빈 리스트 반환)
        List<UsersLinku> candidates = usersLinkuRepository
                .findAllByUserIdAndCreatedAtBetween(userId, monthStart, monthEnd);
        if (candidates.isEmpty()) {
            return List.of();
        }

        // 해당 월 상위 감정 ID 조회
        Optional<Long> topEmotionId = keywordMonthlyCountRepository
                .findTopByUserIdAndBaseMonthAndType(userId, baseMonth, KeywordType.EMOTION, PageRequest.of(0, 1))
                .stream().findFirst()
                .map(KeywordMonthlyCount::getRefId);

        // 해당 월 상위 상황 -> 추천 카테고리 ID 목록 조회
        Optional<KeywordMonthlyCount> topSituation = keywordMonthlyCountRepository
                .findTopByUserIdAndBaseMonthAndType(userId, baseMonth, KeywordType.SITUATION, PageRequest.of(0, 1))
                .stream().findFirst();

        Set<Long> mappedCategoryIds = topSituation.isPresent()
                ? new HashSet<>(situationCategoryRepository.findCategoryIdsBySituationId(topSituation.get().getRefId()))
                : Set.of();

        // 점수 내림차순, 동점 시 최신순으로 정렬 후 limit 추출
        return candidates.stream()
                .sorted(Comparator.comparingInt((UsersLinku link) -> score(link, topEmotionId, mappedCategoryIds))
                        .reversed()
                        .thenComparing(UsersLinku::getCreatedAt, Comparator.reverseOrder()))
                .limit(limit)
                .toList();
    }

    // 스코어링: 감정(최대 60) + 상황(최대 40)
    private int score(UsersLinku link, Optional<Long> topEmotionId, Set<Long> mappedCategoryIds) {
        int emotionScore = EmotionSimilarityUtil.getSimilarityScore(
                topEmotionId.orElse(null),
                link.getEmotion().getEmotionId()
        );

        Long categoryId = (link.getLinku().getCategory() != null)
                ? link.getLinku().getCategory().getCategoryId()
                : null;
        int situationScore = (categoryId != null && mappedCategoryIds.contains(categoryId)) ? 40 : 0;

        return emotionScore + situationScore;
    }
}
