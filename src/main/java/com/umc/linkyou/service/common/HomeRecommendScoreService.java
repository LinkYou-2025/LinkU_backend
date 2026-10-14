package com.umc.linkyou.service.common;

import com.umc.linkyou.config.properties.RecommendScoreProperties;
import com.umc.linkyou.utils.EmotionSimilarityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;

/**
 * 홈화면 링크 추천 스코어링.
 *
 * score = w^T x : 가중치 벡터 w 와 0~1로 정규화된 feature 벡터
 * x = [EmotionMatch, SituationMatch, PersonalEngagement, Popularity, TextMatch, KeywordMatch, CategoryMatch]의 내적.
 *
 * - EmotionMatch   : EmotionSimilarityUtil(0~60점) / 60
 * - SituationMatch : 저장 당시 situation이 요청 situationId와 직접 일치하면 1.0, 아니면 0.
 *                    situation→category 매핑은 별도 축(CategoryMatch)으로 분리돼 있다.
 * - PersonalEngagement : viewCount(재방문 빈도) 정규화 값과 staleness(COALESCE(lastViewedAt, createdAt)가
 *                    오래될수록 1에 가까워지는 값)의 평균. 예전엔 최근에 본 것을 우대했지만(recencyNorm),
 *                    지금은 부호를 뒤집어 "최근에 안 본/생성한 것"을 우대한다 — 방금 저장했거나 방금 본
 *                    링크는 자연히 낮은 점수를 받고, 오래 방치된 링크가 다시 떠오른다(예전 Novelty Quota가
 *                    하드 필터로 하던 일을 이 축의 연속값으로 대체).
 * - Popularity     : Linku.totalViewCount의 로그 정규화 (전역 인기도, 약하게만 반영)
 * - TextMatch      : 후보의 title+summary와 UserContentProfile(사전계산된 유저 프로필)의 Postgres FTS
 *                    (ts_rank_cd) 매칭, 정확히 겹치는 단어가 없으면 pg_trgm similarity()로 fallback
 * - KeywordMatch   : 후보의 linku_keywords와 UserProfileKeyword(사전계산된 유저 키워드 빈도 top-K, K=10)의
 *                    가중치 합을 정규화한 값 — "이 유저가 평소 저장하는 키워드와 얼마나 겹치는가"라는
 *                    intra-user 신호. 협업 필터링(inter-user)용으로 keyword를 쓸 때는 이 신호를 건드리지
 *                    않고 별도 feature를 추가한다 (service/common/README.md 참고).
 * - CategoryMatch  : 후보 Linku.category가 situation→category 매핑(SituationCategoryService)에 걸리면 1.0,
 *                    아니면 0. SituationMatch(저장 당시 태깅)와 달리 순수 콘텐츠 속성(Linku.category)만 보므로
 *                    emotionAi/situationAi 같은 신뢰도 감쇠를 적용하지 않는다.
 *
 * TextMatch/KeywordMatch는 UserProfileRefreshWorker가 비동기로 미리 계산해둔 값을 읽기만 한다 —
 * 요청마다 후보 링크 텍스트를 직접 비교/토큰화하지 않는다.
 *
 * EmotionMatch/SituationMatch는 UsersLinku.emotionAi/situationAi(AI가 추론했는지 여부)에 따라
 * confidence.aiEmotionDiscount/aiSituationDiscount만큼 감쇠된다 — 유저가 직접 고른 값이 AI 추론값보다
 * 신뢰도가 높다고 보고, AI 추론인 경우에만 점수를 깎는다.
 *
 * 가중치·정규화·신뢰도 상수는 RecommendScoreProperties(application.yml: recommend.home.score)에서 관리한다.
 *
 * 7축 전체의 실제 DB 계산은 이 클래스가 아니라 Native SQL(UsersLinkuRepositoryImpl#nativeScoreExpression)
 * 에서 이뤄진다 — 예전엔 이 클래스의 QueryDSL Expression(scoreExpression 등)이 그 역할을 했지만, HQL
 * 파서 중복/OOM 문제로 Native SQL로 옮겨가며 죽은 코드가 됐다(#417, 이후 정리). Novelty Quota(최근에
 * 안 본 것 우선 노출) 버킷이 폐지되면서 그 버킷 전용이던 emotionMatchExpression/situationMatchExpression/
 * noveltyContextScoreExpression/noveltyCondition/scoreBucketExpression도 함께 정리했다 — 이제 모든
 * 후보가 novelty 구분 없이 7축 랭킹 하나로만 정렬된다. 이 클래스에는 배치/테스트 등에서 후보가 이미
 * 메모리에 있을 때 쓰는 Java 스코어링 메서드만 남아 있다.
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
                w.emotion(), w.situation(), w.engagement(), w.popularity(), w.text(), w.keyword(), w.category()
        };
        double[] featureVector = {
                features.emotionMatch(), features.situationMatch(),
                features.personalEngagement(), features.popularity(),
                features.textMatch(), features.keywordMatch(), features.categoryMatch()
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

    /**
     * score() 호출용 feature 묶음. textMatch/keywordMatch는 사전계산된 유저 프로필이 없으면(신규 유저 등)
     * 0.0을 넣으면 된다 — 별도 예외 처리가 필요 없다.
     */
    public record FeatureVector(double emotionMatch, double situationMatch,
                                 double personalEngagement, double popularity,
                                 double textMatch, double keywordMatch, double categoryMatch) {}
}
