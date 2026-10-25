package com.umc.linkyou.service.common;

import com.umc.linkyou.config.properties.RecommendScoreProperties;
import com.umc.linkyou.utils.EmotionSimilarityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 홈 추천은 감정, 상황, 개인 이용 이력, 전체 인기, 카테고리의 5축 가중합으로 계산한다.
 * 실제 DB 랭킹은 UsersLinkuRepositoryImpl의 Native SQL에서 수행한다.
 * 이 클래스는 동일한 가중치로 메모리 내 후보를 평가한다.
 */
@Component
@RequiredArgsConstructor
public class HomeRecommendScoreService {

    private static final int EMOTION_MAX_SCORE = 60;

    private final RecommendScoreProperties properties;

    /** feature vector와 가중치 벡터의 내적(dot product)으로 최종 점수를 계산한다. */
    public double score(FeatureVector features) {
        RecommendScoreProperties.Weight w = properties.weight();
        double[] weightVector = {
                w.emotion(), w.situation(), w.engagement(), w.popularity(), w.category()
        };
        double[] featureVector = {
                features.emotionMatch(), features.situationMatch(),
                features.personalEngagement(), features.popularity(),
                features.categoryMatch()
        };
        return dot(weightVector, featureVector);
    }

    private double dot(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    /**
     * 감정 유사도 (0~1). targetEmotionId 기준으로 candidateEmotionId와의 유사도를 조회한다.
     * candidateEmotionIsAi가 true(=AI가 추론한 감정)면 confidence.aiEmotionDiscount를 곱해 신뢰도를 낮춘다.
     * 유저가 직접 고른 경우(false)는 감쇠 없이 그대로 쓴다.
     */
    public double emotionMatch(Long candidateEmotionId, Long targetEmotionId, boolean candidateEmotionIsAi) {
        double base = EmotionSimilarityUtil.getSimilarityScore(targetEmotionId, candidateEmotionId) / (double) EMOTION_MAX_SCORE;
        return candidateEmotionIsAi ? base * properties.confidence().aiEmotionDiscount() : base;
    }

    /**
     * situation 직접 일치 매칭 (0 / 1.0, AI 추론이면 감쇠).
     * candidateSituationIsAi가 true(=AI가 추론한 situation)면 confidence.aiSituationDiscount를 곱한다.
     * situation→category 매핑 신호는 categoryMatch()가 별도로 담당한다.
     */
    public double situationMatch(Long candidateSituationId, Long targetSituationId,
                                  boolean candidateSituationIsAi) {
        if (candidateSituationId == null || !candidateSituationId.equals(targetSituationId)) {
            return 0.0;
        }
        double discount = candidateSituationIsAi ? properties.confidence().aiSituationDiscount() : 1.0;
        return discount;
    }

    /**
     * situation→category 매핑 (0 / 1.0). 후보 Linku.category가 요청 situationId에 매핑된 category
     * 목록(SituationCategoryService)에 포함되면 1.0, 아니면 0. 저장 당시 태깅이 아닌 콘텐츠 자체의
     * 속성(category)만 보는 신호라 emotionAi/situationAi 신뢰도 감쇠를 적용하지 않는다.
     */
    public double categoryMatch(Long candidateCategoryId, Collection<Long> mappedCategoryIds) {
        if (candidateCategoryId == null || mappedCategoryIds == null) {
            return 0.0;
        }
        return mappedCategoryIds.contains(candidateCategoryId) ? 1.0 : 0.0;
    }

    /**
     * 개인 재방문 빈도(viewCount) + staleness(오래 안 본/안 만든 정도) 정규화 값의 평균 (0~1).
     * lastViewedAt이 없으면(한 번도 안 봄) createdAt 기준으로 얼마나 오래됐는지를 쓴다.
     */
    public double personalEngagement(Integer viewCount, LocalDateTime lastViewedAt, LocalDateTime createdAt, LocalDateTime now) {
        double viewCountNorm = normalizeCapped(viewCount == null ? 0 : viewCount, properties.normalization().viewCountCap());
        LocalDateTime lastTouch = lastViewedAt != null ? lastViewedAt : createdAt;
        double staleness = staleness(lastTouch, now, properties.normalization().recencyHalfLifeDays());
        return (viewCountNorm + staleness) / 2.0;
    }

    /** 전역 인기도 (0~1). 로그 스케일 + 캡으로 popularity bias를 약화시킨다. */
    public double popularity(Long totalViewCount) {
        int cap = properties.normalization().popularityViewCountCap();
        double value = totalViewCount == null ? 0 : totalViewCount;
        double ratio = Math.log(1 + value) / Math.log(1 + cap);
        return Math.min(ratio, 1.0);
    }

    private double normalizeCapped(int value, int cap) {
        if (cap <= 0) return 0.0;
        return Math.min(value, cap) / (double) cap;
    }

    // 0(방금 접촉)에서 1(오래 방치)로 향하는 staleness — 예전 recencyDecay()의 부호를 뒤집은 것.
    private double staleness(LocalDateTime lastTouch, LocalDateTime now, int halfLifeDays) {
        if (lastTouch == null || halfLifeDays <= 0) return 0.0;
        double days = Math.max(Duration.between(lastTouch, now).toHours() / 24.0, 0);
        return 1.0 - Math.exp(-days / halfLifeDays);
    }

    /** 홈 추천의 5개 점수 요소. */
    public record FeatureVector(double emotionMatch, double situationMatch,
                                double personalEngagement, double popularity, double categoryMatch) {}
}
