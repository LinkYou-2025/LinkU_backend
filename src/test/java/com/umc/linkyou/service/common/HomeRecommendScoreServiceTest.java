package com.umc.linkyou.service.common;

import com.umc.linkyou.config.properties.RecommendScoreProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * HomeRecommendScoreService의 Java 메모리 스코어링(*Match, score/FeatureVector)에 대한 단위 테스트.
 * QueryDSL 표현식(*Expression 메서드들)은 실제 Postgres(ts_rank_cd/similarity 등)가 있어야 값을 검증할 수
 * 있어서 UsersLinkuRepositoryImplTest(통합 테스트) 쪽에서 다룬다.
 */
@DisplayName("HomeRecommendScoreService 단위 테스트")
class HomeRecommendScoreServiceTest {

    private static final long EMOTION_JOY = 1L;       // 즐거움
    private static final long EMOTION_CALM = 2L;      // 평온
    private static final long EMOTION_EXCITEMENT = 3L; // 설렘
    private static final long EMOTION_SADNESS = 4L;   // 슬픔

    private static final long SITUATION_A = 10L;
    private static final long SITUATION_B = 20L;
    private static final long CATEGORY_MATCHED = 100L;
    private static final long CATEGORY_UNMATCHED = 200L;

    private final RecommendScoreProperties properties = new RecommendScoreProperties(
            new RecommendScoreProperties.Weight(0.35, 0.15, 0.15, 0.1, 0.1, 0.05, 0.1),
            new RecommendScoreProperties.Normalization(20, 14, 1000, 20),
            new RecommendScoreProperties.Confidence(0.8, 0.8));

    private final HomeRecommendScoreService service = new HomeRecommendScoreService(properties);

    @Nested
    @DisplayName("emotionMatch")
    class EmotionMatchTest {

        @Test
        @DisplayName("정확히 같은 감정이면 1.0(60/60)이다 (유저 직접 선택, 감쇠 없음)")
        void 정확히_같은_감정이면_만점을_받는다() {
            assertThat(service.emotionMatch(EMOTION_JOY, EMOTION_JOY, false)).isEqualTo(1.0);
        }

        @Test
        @DisplayName("유사 감정(40점)이면 40/60로 정규화된다 (유저 직접 선택, 감쇠 없음)")
        void 유사_감정이면_40점_기준으로_정규화된다() {
            assertThat(service.emotionMatch(EMOTION_EXCITEMENT, EMOTION_JOY, false))
                    .isCloseTo(40.0 / 60.0, within(1e-9));
        }

        @Test
        @DisplayName("관계 없는 감정이면 0이다")
        void 관계_없는_감정이면_0점이다() {
            assertThat(service.emotionMatch(EMOTION_SADNESS, EMOTION_JOY, false)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("AI가 추론한 감정이면 aiEmotionDiscount(0.8)만큼 감쇠된다")
        void AI가_추론한_감정이면_감쇠된다() {
            double userChosen = service.emotionMatch(EMOTION_JOY, EMOTION_JOY, false);
            double aiInferred = service.emotionMatch(EMOTION_JOY, EMOTION_JOY, true);
            assertThat(aiInferred).isCloseTo(userChosen * 0.8, within(1e-9));
        }
    }

    @Nested
    @DisplayName("situationMatch")
    class SituationMatchTest {

        @Test
        @DisplayName("저장 당시 situation이 요청 situationId와 직접 일치하면 1.0이다 (유저 직접 선택)")
        void situation이_직접_일치하면_만점을_받는다() {
            double result = service.situationMatch(SITUATION_A, SITUATION_A, false);
            assertThat(result).isEqualTo(1.0);
        }

        @Test
        @DisplayName("일치하지 않으면 0이다")
        void 일치하지_않으면_0점이다() {
            double result = service.situationMatch(SITUATION_B, SITUATION_A, false);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("candidateSituationId가 null이어도 예외 없이 0이다")
        void candidateSituationId가_null이어도_0점이다() {
            double result = service.situationMatch(null, SITUATION_A, false);
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("AI가 추론한 situation이면 aiSituationDiscount(0.8)만큼 감쇠된다")
        void AI가_추론한_situation이면_감쇠된다() {
            double userChosen = service.situationMatch(SITUATION_A, SITUATION_A, false);
            double aiInferred = service.situationMatch(SITUATION_A, SITUATION_A, true);
            assertThat(aiInferred).isCloseTo(userChosen * 0.8, within(1e-9));
        }
    }

    @Nested
    @DisplayName("categoryMatch")
    class CategoryMatchTest {

        @Test
        @DisplayName("후보 category가 매핑 목록에 있으면 1.0이다")
        void 매핑_목록에_있으면_1점이다() {
            double result = service.categoryMatch(CATEGORY_MATCHED, List.of(CATEGORY_MATCHED));
            assertThat(result).isEqualTo(1.0);
        }

        @Test
        @DisplayName("후보 category가 매핑 목록에 없으면 0이다")
        void 매핑_목록에_없으면_0점이다() {
            double result = service.categoryMatch(CATEGORY_UNMATCHED, List.of(CATEGORY_MATCHED));
            assertThat(result).isEqualTo(0.0);
        }

        @Test
        @DisplayName("candidateCategoryId 또는 mappedCategoryIds가 null이어도 예외 없이 0이다")
        void 입력값이_null이어도_0점이다() {
            assertThat(service.categoryMatch(null, List.of(CATEGORY_MATCHED))).isEqualTo(0.0);
            assertThat(service.categoryMatch(CATEGORY_MATCHED, null)).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("personalEngagement")
    class PersonalEngagementTest {

        private final LocalDateTime now = LocalDateTime.of(2026, 7, 24, 12, 0);

        @Test
        @DisplayName("viewCount가 0이고 방금 저장/조회한 것이면(staleness=0) 0이다")
        void viewCount가_0이고_방금_생성됐으면_0점이다() {
            assertThat(service.personalEngagement(0, null, now, now)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("viewCount가 cap 이상이면 빈도 항목은 1.0으로 캡핑된다")
        void viewCount가_cap_이상이면_1점으로_캡핑된다() {
            double atCap = service.personalEngagement(20, null, now, now);
            double overCap = service.personalEngagement(100, null, now, now);
            assertThat(atCap).isEqualTo(overCap); // 둘 다 캡에 걸려 빈도 항목은 0.5(=1.0/2)로 동일
        }

        @Test
        @DisplayName("오래 안 볼수록(=staleness가 높을수록) 점수가 더 높다 — 최근에 본 것과 반대 방향")
        void 오래_안_볼수록_점수가_더_높다() {
            double recent = service.personalEngagement(0, now.minusDays(1), now.minusDays(60), now);
            double old = service.personalEngagement(0, now.minusDays(30), now.minusDays(60), now);
            assertThat(old).isGreaterThan(recent);
        }

        @Test
        @DisplayName("한 번도 안 봤으면(lastViewedAt=null) createdAt 기준으로 staleness를 계산한다")
        void lastViewedAt이_null이면_createdAt_기준으로_staleness를_계산한다() {
            double justCreated = service.personalEngagement(0, null, now, now);
            double oldCreated = service.personalEngagement(0, null, now.minusDays(30), now);
            assertThat(oldCreated).isGreaterThan(justCreated);
        }
    }

    @Nested
    @DisplayName("popularity")
    class PopularityTest {

        @Test
        @DisplayName("totalViewCount가 null이면 0이다")
        void totalViewCount가_null이면_0점이다() {
            assertThat(service.popularity(null)).isEqualTo(0.0);
        }

        @Test
        @DisplayName("totalViewCount가 늘어날수록 점수도 증가하지만 1.0을 넘지 않는다")
        void totalViewCount가_늘어나도_1점을_넘지_않는다() {
            double low = service.popularity(10L);
            double high = service.popularity(100_000L);
            assertThat(low).isGreaterThan(0.0);
            assertThat(high).isLessThanOrEqualTo(1.0);
            assertThat(high).isGreaterThan(low);
        }
    }

    @Nested
    @DisplayName("score (feature vector 내적)")
    class ScoreTest {

        @Test
        @DisplayName("각 feature * 가중치의 합과 정확히 같다")
        void 각_feature_가중치의_합과_같다() {
            HomeRecommendScoreService.FeatureVector features =
                    new HomeRecommendScoreService.FeatureVector(1.0, 1.0, 0.5, 0.2, 0.3, 0.1, 1.0);

            double expected = 1.0 * 0.35 + 1.0 * 0.15 + 0.5 * 0.15 + 0.2 * 0.1 + 0.3 * 0.1 + 0.1 * 0.05 + 1.0 * 0.1;

            assertThat(service.score(features)).isCloseTo(expected, within(1e-9));
        }

        @Test
        @DisplayName("모든 feature가 0이면 최종 점수도 0이다")
        void 모든_feature가_0이면_최종_점수도_0이다() {
            HomeRecommendScoreService.FeatureVector features =
                    new HomeRecommendScoreService.FeatureVector(0, 0, 0, 0, 0, 0, 0);
            assertThat(service.score(features)).isEqualTo(0.0);
        }
    }
}
