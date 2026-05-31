package com.umc.linkyou.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmotionSimilarityUtilTest {
    private static final long EMOTION_JOY = 1L; // 즐거움
    private static final long EMOTION_CALM = 2L; // 평온
    private static final long EMOTION_EXCITEMENT = 3L; // 설렘
    private static final long EMOTION_SADNESS = 4L; // 슬픔
    private static final long EMOTION_ANNOYANCE = 5L; // 짜증
    private static final long EMOTION_ANGER = 6L; // 분노

    @Test
    @DisplayName("동일 감정이면 60점을 반환한다")
    void exactMatch_returns60() {
        for (long id = EMOTION_JOY; id <= EMOTION_ANGER; id++) {
            assertThat(EmotionSimilarityUtil.getSimilarityScore(id, id)).isEqualTo(60);
        }
    }

    @Test
    @DisplayName("유사 감정이면 40점을 반환한다 - 즐거움(1) 기준 설렘(3)")
    void similarMatch_returns40() {
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_JOY, EMOTION_EXCITEMENT)).isEqualTo(40); // 즐거움→설렘
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_EXCITEMENT, EMOTION_JOY)).isEqualTo(40); // 설렘→즐거움
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_ANNOYANCE, EMOTION_ANGER)).isEqualTo(40); // 짜증→분노
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_ANGER, EMOTION_ANNOYANCE)).isEqualTo(40); // 분노→짜증
    }

    @Test
    @DisplayName("약한 연관 감정이면 20점을 반환한다")
    void weakMatch_returns20() {
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_JOY, EMOTION_CALM)).isEqualTo(20); // 즐거움→평온
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_EXCITEMENT, EMOTION_CALM)).isEqualTo(20); // 설렘→평온
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_SADNESS, EMOTION_ANNOYANCE)).isEqualTo(20); // 슬픔→짜증
    }

    @Test
    @DisplayName("관계 없는 감정이면 0점을 반환한다")
    void noRelation_returns0() {
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_JOY, EMOTION_SADNESS)).isEqualTo(0); // 즐거움→슬픔
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_JOY, EMOTION_ANNOYANCE)).isEqualTo(0); // 즐거움→짜증
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_JOY, EMOTION_ANGER)).isEqualTo(0); // 즐거움→분노
    }

    @Test
    @DisplayName("감정 ID가 null이면 0점을 반환한다")
    void nullEmotionId_returns0() {
        assertThat(EmotionSimilarityUtil.getSimilarityScore(null, EMOTION_JOY)).isEqualTo(0);
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_JOY, null)).isEqualTo(0);
        assertThat(EmotionSimilarityUtil.getSimilarityScore(null, null)).isEqualTo(0);
    }

    @Test
    @DisplayName("점수 비대칭 - 즐거움(1)→평온(2)은 20점, 평온(2)→즐거움(1)은 40점")
    void asymmetric_scoring() {
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_JOY, EMOTION_CALM)).isEqualTo(20);
        assertThat(EmotionSimilarityUtil.getSimilarityScore(EMOTION_CALM, EMOTION_JOY)).isEqualTo(40);
    }
}
