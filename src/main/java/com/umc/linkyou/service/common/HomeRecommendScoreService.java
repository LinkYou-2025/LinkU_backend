package com.umc.linkyou.service.common;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.umc.linkyou.config.properties.RecommendScoreProperties;
import com.umc.linkyou.domain.QAiArticle;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.mapping.QLinkuKeyword;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.domain.recommend.QUserProfileKeyword;
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
 * - PersonalEngagement : viewCount(재방문 빈도)와 lastViewedAt(최신성) 정규화 값의 평균
 * - Popularity     : Linku.totalViewCount의 로그 정규화 (전역 인기도, 약하게만 반영)
 * - TextMatch      : 후보의 title+summary와 UserContentProfile(사전계산된 유저 프로필)의 Postgres FTS
 *                    (ts_rank_cd) 매칭, 정확히 겹치는 단어가 없으면 pg_trgm similarity()로 fallback
 * - KeywordMatch   : 후보의 linku_keywords와 UserProfileKeyword(사전계산된 유저 키워드 빈도 top-K)의
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
 */
@Component
@RequiredArgsConstructor
public class HomeRecommendScoreService {

    private static final int EMOTION_MAX_SCORE = 60;
    /** ts_rank_cd가 0(정확히 겹치는 단어 없음)일 때 pg_trgm similarity() fallback에 곱하는 감쇠 계수 */
    private static final double TRGM_FALLBACK_DAMPENING = 0.7;
    /** score(0~1대)를 나눌 구간 수. 200이면 해상도 0.005로 충분히 촘촘함 */
    private static final int SCORE_BUCKET_COUNT = 200;

    private final RecommendScoreProperties properties;

    // =====================================================================
    // Java 메모리 스코어링 (배치/테스트 등 후보가 이미 메모리에 있는 경우)
    // =====================================================================

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

    /** 개인 재방문 빈도(viewCount) + 최신성(lastViewedAt) 정규화 값의 평균 (0~1). */
    public double personalEngagement(Integer viewCount, LocalDateTime lastViewedAt, LocalDateTime now) {
        double viewCountNorm = normalizeCapped(viewCount == null ? 0 : viewCount, properties.normalization().viewCountCap());
        double recencyNorm = recencyDecay(lastViewedAt, now, properties.normalization().recencyHalfLifeDays());
        return (viewCountNorm + recencyNorm) / 2.0;
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

    private double recencyDecay(LocalDateTime lastViewedAt, LocalDateTime now, int halfLifeDays) {
        if (lastViewedAt == null || halfLifeDays <= 0) return 0.0;
        double days = Math.max(Duration.between(lastViewedAt, now).toHours() / 24.0, 0);
        return Math.exp(-days / halfLifeDays);
    }

    /**
     * score() 호출용 feature 묶음. textMatch/keywordMatch는 사전계산된 유저 프로필이 없으면(신규 유저 등)
     * 0.0을 넣으면 된다 — 별도 예외 처리가 필요 없다.
     */
    public record FeatureVector(double emotionMatch, double situationMatch,
                                 double personalEngagement, double popularity,
                                 double textMatch, double keywordMatch, double categoryMatch) {}

    // =====================================================================
    // QueryDSL 표현식 (홈화면 추천 - DB에서 정렬/페이징까지 한 번에 처리)
    // =====================================================================

    /**
     * 최종 스코어 표현식. ORDER BY 절에 그대로 사용한다.
     *
     * 주의:
     * - usersLinku.situation 경로를 조건절에서 참조하므로, 호출하는 쿼리에서 반드시
     *   {@code .leftJoin(usersLinku.situation)} 를 걸어야 한다. 명시적 LEFT JOIN 없이 경로만 참조하면
     *   JPQL이 암시적 INNER JOIN으로 해석해 situation이 없는(null) 저장 링크가 결과에서 통째로 빠지는
     *   버그가 생긴다.
     * - aiArticle도 마찬가지로 호출하는 쿼리에서 {@code .leftJoin(linku.aiArticle, aiArticle)}이 필요하다
     *   (summary가 없는 링크도 있어서 INNER JOIN이면 안 된다).
     *
     * @param profileTsqueryText UserContentProfile.profileTsqueryText (없으면 null — TextMatch는 0 처리)
     * @param profileText        UserContentProfile.profileText (trgm fallback용, 없으면 null)
     */
    public NumberExpression<Double> scoreExpression(
            QUsersLinku usersLinku, QLinku linku, QAiArticle aiArticle,
            Long userId, Long targetEmotionId, Long targetSituationId, Collection<Long> mappedCategoryIds,
            LocalDateTime now, String profileTsqueryText, String profileText) {

        RecommendScoreProperties.Weight w = properties.weight();

        return emotionMatchExpression(usersLinku, targetEmotionId).multiply(w.emotion())
                .add(situationMatchExpression(usersLinku, targetSituationId).multiply(w.situation()))
                .add(personalEngagementExpression(usersLinku, now).multiply(w.engagement()))
                .add(popularityExpression(linku).multiply(w.popularity()))
                .add(textMatchExpression(linku, aiArticle, profileTsqueryText, profileText).multiply(w.text()))
                .add(keywordMatchExpression(linku, userId).multiply(w.keyword()))
                .add(categoryMatchExpression(linku, mappedCategoryIds).multiply(w.category()));
    }

    /**
     * score를 SCORE_BUCKET_COUNT 구간으로 내림 양자화 — seek 페이징의 정렬/탐색 키.
     * score는 매 요청 SQL로 재계산돼 미세하게 흔들릴 수 있는데(recencyDecay, viewCount 등),
     * 버킷 단위로 뭉개면 이 흔들림이 흡수돼 커서 안정성이 생긴다. 버킷 내 동점은 userLinkuId로 타이브레이크.
     *
     * 구현 주의: score(scoreExpression/noveltyContextScoreExpression)는 여러 항을 {@code .add()}/
     * raw 템플릿({@code numberTemplate}으로 이 중첩 트리를 {0} 자리에 통으로 밀어 넣는 방식)은
     * Hibernate 6 HQL 분석기가 타입을 못 잡아 SemanticException을 던진다(재현됨) — 네이티브 연산자만 사용.
     * FLOOR 불필요: score는 항상 0 이상(전 항이 CASE WHEN otherwise 0/1.0)이라 intValue() 절삭 = floor.
     */
    public NumberExpression<Integer> scoreBucketExpression(NumberExpression<Double> score) {
        return score.multiply((double) SCORE_BUCKET_COUNT).intValue();
    }

    /**
     * 감정 유사도 점수를 CASE WHEN 식으로 변환한 뒤 0~1로 정규화한다.
     * EmotionSimilarityUtil의 (선택 감정, 후보 감정) 매핑을 그대로 SQL로 옮긴 것으로,
     * 실제 점수 값의 출처는 여전히 EmotionSimilarityUtil 하나다.
     * usersLinku.emotionAi가 true(AI 추론)면 confidence.aiEmotionDiscount를 곱하고,
     * false(유저 직접 선택)면 감쇠 없이 그대로 쓴다.
     */
    public NumberExpression<Double> emotionMatchExpression(QUsersLinku usersLinku, Long targetEmotionId) {
        CaseBuilder.Cases<Integer, NumberExpression<Integer>> chain = null;

        for (long candidateEmotionId = 1; candidateEmotionId <= 6; candidateEmotionId++) {
            int emotionScore = EmotionSimilarityUtil.getSimilarityScore(targetEmotionId, candidateEmotionId);
            if (emotionScore == 0) {
                continue;
            }
            BooleanExpression condition = usersLinku.emotion.emotionId.eq(candidateEmotionId);
            chain = (chain == null)
                    ? new CaseBuilder().when(condition).then(emotionScore)
                    : chain.when(condition).then(emotionScore);
        }

        NumberExpression<Integer> raw = chain != null ? chain.otherwise(0) : Expressions.asNumber(0);
        NumberExpression<Double> normalized = raw.doubleValue().divide((double) EMOTION_MAX_SCORE);
        double aiDiscount = properties.confidence().aiEmotionDiscount();

        NumberExpression<Double> discountFactor = new CaseBuilder()
                .when(usersLinku.emotionAi.isTrue()).then(aiDiscount)
                .otherwise(1.0);

        return normalized.multiply(discountFactor);
    }

    /**
     * situation 직접 일치 점수를 CASE WHEN 식으로 변환한다 (0 또는 1.0).
     * usersLinku.situationAi가 true(AI 추론)면 confidence.aiSituationDiscount를 곱하고,
     * false(유저 직접 선택)면 감쇠 없이 그대로 쓴다. situation→category 매핑은
     * categoryMatchExpression()이 별도로 담당한다.
     */
    public NumberExpression<Double> situationMatchExpression(QUsersLinku usersLinku, Long targetSituationId) {
        NumberExpression<Double> directMatch = new CaseBuilder()
                .when(usersLinku.situation.id.eq(targetSituationId)).then(1.0)
                .otherwise(0.0);

        double aiDiscount = properties.confidence().aiSituationDiscount();
        NumberExpression<Double> discountFactor = new CaseBuilder()
                .when(usersLinku.situationAi.isTrue()).then(aiDiscount)
                .otherwise(1.0);

        return directMatch.multiply(discountFactor);
    }

    /**
     * situation→category 매핑 점수를 CASE WHEN 식으로 변환한다 (0 또는 1.0).
     * 후보 Linku.category가 요청 situationId에 매핑된 category 목록에 포함되면 1.0, 아니면 0.
     * 저장 당시 태깅이 아닌 콘텐츠 속성(category)만 보는 신호라 신뢰도 감쇠를 적용하지 않는다.
     */
    public NumberExpression<Double> categoryMatchExpression(QLinku linku, Collection<Long> mappedCategoryIds) {
        if (mappedCategoryIds == null || mappedCategoryIds.isEmpty()) {
            return Expressions.asNumber(0.0);
        }
        return new CaseBuilder()
                .when(linku.category.categoryId.in(mappedCategoryIds)).then(1.0)
                .otherwise(0.0);
    }

    /** viewCount(빈도) + lastViewedAt(최신성) 정규화 값의 평균. */
    public NumberExpression<Double> personalEngagementExpression(QUsersLinku usersLinku, LocalDateTime now) {
        double cap = properties.normalization().viewCountCap();
        double halfLife = properties.normalization().recencyHalfLifeDays();

        NumberExpression<Double> viewCountNorm = Expressions.numberTemplate(Double.class,
                "LEAST(CAST({0} AS double), {1}) / {1}",
                usersLinku.viewCount, cap);

        NumberExpression<Double> recencyNorm = Expressions.numberTemplate(Double.class,
                "CASE WHEN {0} IS NULL THEN 0.0 "
                        + "ELSE EXP(-GREATEST(TIMESTAMPDIFF(SECOND, {0}, {2}) / 86400.0, 0) / {1}) END",
                usersLinku.lastViewedAt, halfLife, now);

        return viewCountNorm.add(recencyNorm).divide(2.0);
    }

    /** Linku.totalViewCount 로그 정규화 (전역 인기도). */
    public NumberExpression<Double> popularityExpression(QLinku linku) {
        double cap = properties.normalization().popularityViewCountCap();

        NumberExpression<Double> ratio = Expressions.numberTemplate(Double.class,
                "LN(1 + CAST({0} AS double)) / LN(1 + {1})",
                linku.totalViewCount, cap);

        return Expressions.numberTemplate(Double.class, "LEAST({0}, 1.0)", ratio);
    }

    /**
     * title+summary 기반 TextMatch. UserContentProfile에 미리 계산해둔 profileTsqueryText로
     * ts_rank_cd를 계산해 rank/(rank+1)로 0~1에 눌러 담고, 정확히 겹치는 단어가 없으면(=0이면)
     * profileText와의 pg_trgm similarity()를 TRGM_FALLBACK_DAMPENING만큼 감쇠해서 대신 쓴다.
     * profileTsqueryText/profileText가 둘 다 없으면(프로필 미생성 유저) 0을 반환한다.
     */
    public NumberExpression<Double> textMatchExpression(
            QLinku linku, QAiArticle aiArticle, String profileTsqueryText, String profileText) {

        if (profileTsqueryText == null && profileText == null) {
            return Expressions.asNumber(0.0);
        }

        // ts_rank_cd/to_tsvector/to_tsquery/similarity는 Hibernate가 반환 타입을 모르는
        // Postgres 전용 함수라, 여기서 나온 값을 그대로 +/>로 연산하면 Hibernate 6 HQL 분석기가
        // 피연산자를 java.lang.Object로 취급해 "Operand of + is of type java.lang.Object"
        // SemanticException을 던진다(재현됨 — profileTsqueryText/profileText가 null이 아닌
        // 실제 유저에서만 이 분기를 타서, 둘 다 null로 두는 테스트에서는 드러나지 않았다).
        // usersLinku.viewCount/linku.totalViewCount를 CAST({0} AS double)로 감싸는 것과 동일하게,
        // 함수 호출 결과를 산술/비교에 쓰기 전에 명시적으로 CAST해서 타입을 고정해야 한다.
        NumberExpression<Double> ftsScore = Expressions.numberTemplate(Double.class,
                "CASE WHEN {2} IS NULL OR {2} = '' THEN 0.0 ELSE ("
                        + "CAST(ts_rank_cd(to_tsvector('simple', {0} || ' ' || COALESCE({1}, '')), to_tsquery('simple', {2})) AS double) / "
                        + "(CAST(ts_rank_cd(to_tsvector('simple', {0} || ' ' || COALESCE({1}, '')), to_tsquery('simple', {2})) AS double) + 1.0)"
                        + ") END",
                linku.title, aiArticle.summary, profileTsqueryText);

        NumberExpression<Double> trgmScore = Expressions.numberTemplate(Double.class,
                "CAST(COALESCE(similarity({0} || ' ' || COALESCE({1}, ''), {2}), 0) AS double)",
                linku.title, aiArticle.summary, profileText);

        return Expressions.numberTemplate(Double.class,
                "CASE WHEN {0} > 0 THEN {0} ELSE {1} * {2} END",
                ftsScore, trgmScore, TRGM_FALLBACK_DAMPENING);
    }

    // =====================================================================
    // Novelty(최근에 안 본 것) 버킷 — QueryDSL 표현식
    // =====================================================================

    /**
     * "최근에 안 본" 후보 조건. lastViewedAt이 있으면 마지막으로 본 지 recencyThresholdDays 넘었는지,
     * null(한 번도 안 봄)이면 저장한 지(createdAt) recencyThresholdDays 넘었는지를 같은 기준으로 본다
     * — COALESCE(lastViewedAt, createdAt) < now - recencyThresholdDays. 방금 저장해서 아직 볼 기회가
     * 없었던 링크가 novelty로 잡히는 것을 막기 위해 createdAt 기준을 함께 쓴다
     * (service/common/README.md "novelty quota" 참고).
     */
    public BooleanExpression noveltyCondition(QUsersLinku usersLinku, LocalDateTime now, int recencyThresholdDays) {
        LocalDateTime threshold = now.minusDays(recencyThresholdDays);
        return Expressions.booleanTemplate(
                "COALESCE({0}, {1}) < {2}",
                usersLinku.lastViewedAt, usersLinku.createdAt, threshold);
    }

    /** normal 버킷(가중합 랭킹)에서 novelty 후보를 제외하기 위한 반대 조건. 두 버킷을 서로소로 유지한다. */
    public BooleanExpression notNoveltyCondition(QUsersLinku usersLinku, LocalDateTime now, int recencyThresholdDays) {
        return noveltyCondition(usersLinku, now, recencyThresholdDays).not();
    }

    /**
     * novelty 버킷 전용 정렬 스코어. 7축 가중합이 아니라 EmotionMatch/SituationMatch 두 축만 재사용해서
     * "유저가 지금 고른 감정/상황과 얼마나 맞는가"만으로 정렬한다. emotionMatchExpression/
     * situationMatchExpression을 그대로 재사용하므로 AI 추론 신뢰도 감쇠(aiEmotionDiscount/
     * aiSituationDiscount)도 동일하게 적용된다.
     */
    public NumberExpression<Double> noveltyContextScoreExpression(
            QUsersLinku usersLinku, Long targetEmotionId, Long targetSituationId) {
        RecommendScoreProperties.Weight w = properties.weight();
        return emotionMatchExpression(usersLinku, targetEmotionId).multiply(w.emotion())
                .add(situationMatchExpression(usersLinku, targetSituationId).multiply(w.situation()));
    }

    /**
     * 후보 링크의 linku_keywords와 이 유저의 UserProfileKeyword(상위 키워드 빈도)를 스칼라 서브쿼리로
     * 겹쳐서 weight 합을 구하고, keywordWeightCap으로 정규화한다. JOIN + GROUP BY로 하면 메인 쿼리의
     * fetch join 구조(행당 1건 보장)가 깨지므로 서브쿼리로 처리한다.
     */
    public NumberExpression<Double> keywordMatchExpression(QLinku linku, Long userId) {
        if (userId == null) {
            return Expressions.asNumber(0.0);
        }

        QLinkuKeyword linkuKeyword = QLinkuKeyword.linkuKeyword;
        QUserProfileKeyword profileKeyword = QUserProfileKeyword.userProfileKeyword;
        double cap = properties.normalization().keywordWeightCap();

        // 서브쿼리 자체를 NumberExpression으로 담지 않고 Object 인자로 바로 넘긴다
        // (JPAQuery는 Expression이긴 하지만 NumberExpression의 산술 연산까지는 지원하지 않는다).
        var matchedWeightSum = JPAExpressions
                .select(profileKeyword.weight.sum())
                .from(linkuKeyword)
                .join(profileKeyword).on(profileKeyword.keywordId.eq(linkuKeyword.keyword.id))
                .where(linkuKeyword.linku.eq(linku), profileKeyword.userId.eq(userId));

        // 서브쿼리 결과를 COALESCE/LEAST에 바로 섞어 넣으면(구 버전) Hibernate 6 HQL 분석기가
        // 이 표현식이 select 절(findNormalRecommendCandidates의 select(usersLinku, bucket))에
        // 들어갈 때 서브쿼리 반환 타입을 못 잡고 java.lang.Object로 취급해
        // "Operand ... is of type java.lang.Object which is not a numeric type" SemanticException을
        // 던진다(재현됨). personalEngagementExpression/popularityExpression에서 plain path를
        // CAST({0} AS double)로 감싸는 것과 동일하게, 서브쿼리도 먼저 CAST로 타입을 명시해야 한다.
        NumberExpression<Double> matchedWeight = Expressions.numberTemplate(Double.class,
                "CAST(COALESCE(({0}), 0) AS double)", matchedWeightSum);

        return Expressions.numberTemplate(Double.class,
                "LEAST({0}, {1}) / {1}",
                matchedWeight, cap);
    }
}
