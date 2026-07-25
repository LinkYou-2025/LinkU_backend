package com.umc.linkyou.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 홈화면 링크 추천 스코어링 가중치 · 정규화 상수.
 * HomeRecommendScoreService가 이 값을 읽어 feature vector(x)에 가중치 벡터(w)를 내적한다: score = w^T x
 *
 * application.yml: recommend.home.score.*
 */
@ConfigurationProperties("recommend.home.score")
public record RecommendScoreProperties(
        Weight weight,
        Normalization normalization,
        Confidence confidence
) {
    /**
     * feature별 가중치. 합이 1일 필요는 없지만(내적일 뿐이므로), 튜닝 편의상 1로 맞추는 걸 권장한다.
     * text/keyword는 사전계산된 유저 프로필(user_content_profiles/user_profile_keywords)이 있어야 값이
     * 나오므로, 초기값은 작게 잡고 실측 후 튜닝한다.
     */
    public record Weight(double emotion, double situation, double engagement, double popularity,
                          double text, double keyword, double category) {}

    /** 0~1 정규화에 쓰이는 캡/half-life 값 */
    public record Normalization(int viewCountCap, int recencyHalfLifeDays, int popularityViewCountCap,
                                 int keywordWeightCap) {}

    /**
     * 유저가 직접 고른 감정/situation보다 AI가 추론한 값의 신뢰도를 낮게 보고 곱하는 감쇠 계수(0~1).
     * UsersLinku.emotionAi/situationAi가 true(AI 추론)면 이 값을, false(유저 직접 선택)면 1.0을 곱한다.
     */
    public record Confidence(double aiEmotionDiscount, double aiSituationDiscount) {}
}
