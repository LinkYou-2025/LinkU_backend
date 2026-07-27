package com.umc.linkyou.repository.UserLinkuRepository;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.categoryRepository.FcolorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.config.TestExternalConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * UsersLinkuRepositoryImpl#findHomeRecommendCandidates 통합 테스트.
 *
 * TextMatch/KeywordMatch용 profileTsqueryText/profileText는 전부 null로 넘긴다 — 둘 다 null이면
 * HomeRecommendScoreService#textMatchExpression이 similarity()/ts_rank_cd를 아예 호출하지 않는
 * 상수(0.0) 표현식만 반환하므로, pg_trgm 확장 설치 여부와 무관하게 이 테스트가 검증하려는
 * EmotionMatch/SituationMatch/PersonalEngagement/Popularity 동작에 집중할 수 있다.
 *
 * 감정(EmotionMatch) 자체는 EmotionSimilarityUtil이 감정 ID 1~6을 하드코딩 매핑하고 있어서, 테스트
 * DB에서 매번 새로 생성되는(IDENTITY 시퀀스가 테스트 클래스 간에도 누적되는) Emotion의 실제 ID가 1~6
 * 범위를 벗어나면 EmotionMatch가 항상 0이 되어 이 값 자체로는 정렬을 검증할 수 없다. 그래서 이 테스트는
 * 모든 후보에 같은 Emotion 엔티티를 써서 EmotionMatch 항을 동일하게 고정해두고, SituationMatch/
 * PersonalEngagement/Popularity 차이로만 정렬 순서를 검증한다.
 */
@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
@DisplayName("UsersLinkuRepositoryImpl#findHomeRecommendCandidates 테스트")
class UsersLinkuRepositoryImplTest {

    @Autowired private UsersLinkuRepository usersLinkuRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DomainRepository domainRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private FcolorRepository fcolorRepository;
    @Autowired private EmotionRepository emotionRepository;
    @Autowired private SituationRepository situationRepository;
    @Autowired private LinkuRepository linkuRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    // BaseEntity.createdAt은 @CreatedDate(updatable=false)라 JPA로는 저장 후 값을 바꿀 수 없어서,
    // "저장한 지 오래된" 상태를 재현하려면 raw SQL로 직접 덮어써야 한다
    // (DeleteOldAlarmBatchIntegrationTest#saveAlarm과 같은 패턴).
    private void overrideCreatedAt(Long userLinkuId, LocalDateTime createdAt) {
        jdbcTemplate.update("UPDATE users_linkus SET created_at = ? WHERE user_linku_id = ?", createdAt, userLinkuId);
    }

    @Nested
    @DisplayName("novelty(최근에 안 본 것) 버킷")
    class NoveltyBucket {

        @Test
        @DisplayName("findNoveltyRecommendCandidates는 COALESCE(lastViewedAt, createdAt)가 임계값보다 오래된 후보만 반환한다")
        void onlyReturnsCandidatesOlderThanThreshold() {
            Users user = userRepository.save(createUser("home-reco-novelty"));
            Domain domain = domainRepository.save(createDomain("novelty-test"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("카테고리", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation("상황"));

            LocalDateTime now = LocalDateTime.now();
            int recencyThresholdDays = 14;

            // 한 번도 안 봄(lastViewedAt=null) + 저장한 지 30일 지남 → novelty 대상
            Linku linkuNeverViewedOld = createAndSaveLinku(domain, category, emotion, situation);
            UsersLinku neverViewedOld = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuNeverViewedOld, emotion).situation(situation).build());
            overrideCreatedAt(neverViewedOld.getUserLinkuId(), now.minusDays(30));

            // 한 번도 안 봄 + 방금 저장(기본 createdAt=now) → 아직 볼 기회가 없었을 뿐이라 novelty 아님
            Linku linkuNeverViewedRecent = createAndSaveLinku(domain, category, emotion, situation);
            usersLinkuRepository.save(
                    baseUsersLinku(user, linkuNeverViewedRecent, emotion).situation(situation).build());

            // 마지막으로 본 지 30일 지남 → novelty 대상
            Linku linkuViewedOld = createAndSaveLinku(domain, category, emotion, situation);
            UsersLinku viewedOld = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuViewedOld, emotion).situation(situation)
                            .lastViewedAt(now.minusDays(30)).build());

            // 마지막으로 본 지 1일밖에 안 지남 → novelty 아님
            Linku linkuViewedRecent = createAndSaveLinku(domain, category, emotion, situation);
            usersLinkuRepository.save(
                    baseUsersLinku(user, linkuViewedRecent, emotion).situation(situation)
                            .lastViewedAt(now.minusDays(1)).build());

            List<UsersLinku> result = usersLinkuRepository.findNoveltyRecommendCandidates(
                    user.getId(), emotion.getEmotionId(), situation.getId(), now, recencyThresholdDays, 0, 10);

            assertThat(result).extracting(UsersLinku::getUserLinkuId)
                    .containsExactlyInAnyOrder(neverViewedOld.getUserLinkuId(), viewedOld.getUserLinkuId());
        }

        @Test
        @DisplayName("findNormalRecommendCandidates는 novelty 대상을 제외한다 (서로소 유지)")
        void normalBucketExcludesNoveltyCandidates() {
            Users user = userRepository.save(createUser("home-reco-normal"));
            Domain domain = domainRepository.save(createDomain("normal-test"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("카테고리", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation("상황"));

            LocalDateTime now = LocalDateTime.now();
            int recencyThresholdDays = 14;

            // novelty 대상(마지막으로 본 지 30일 지남) — normal 버킷에는 안 나와야 함
            Linku linkuNovelty = createAndSaveLinku(domain, category, emotion, situation);
            usersLinkuRepository.save(
                    baseUsersLinku(user, linkuNovelty, emotion).situation(situation)
                            .lastViewedAt(now.minusDays(30)).build());

            // novelty 아님(최근에 봄) — normal 버킷에 나와야 함
            Linku linkuNormal = createAndSaveLinku(domain, category, emotion, situation);
            UsersLinku normal = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuNormal, emotion).situation(situation)
                            .lastViewedAt(now.minusDays(1)).build());

            List<UsersLinku> result = usersLinkuRepository.findNormalRecommendCandidates(
                    user.getId(), emotion.getEmotionId(), situation.getId(), List.of(category.getCategoryId()),
                    now, null, null, recencyThresholdDays, 0, 10);

            assertThat(result).extracting(UsersLinku::getUserLinkuId)
                    .containsExactly(normal.getUserLinkuId());
        }
    }

    @Nested
    @DisplayName("SituationMatch")
    class SituationMatchOrdering {

        @Test
        @DisplayName("직접 일치 > category 매핑만 일치 > 매칭 없음 순으로 정렬되고, situation=null인 후보도 결과에서 빠지지 않는다")
        void directMatchBeatsCategoryMatchBeatsNone() {
            Users user = userRepository.save(createUser("home-reco-situation"));
            Domain domain = domainRepository.save(createDomain("situation-test"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category matchedCategory = categoryRepository.save(createCategory("매칭카테고리", fcolor));
            Category unmatchedCategory = categoryRepository.save(createCategory("비매칭카테고리", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());

            Situation targetSituation = situationRepository.save(createSituation("타깃상황"));
            Situation otherSituation = situationRepository.save(createSituation("다른상황"));

            // 직접 일치: situation == targetSituation. category는 일부러 안 맞춰서, 점수가 순수 direct
            // match(1.0)에서만 나오는지 확인한다.
            Linku linkuDirect = createAndSaveLinku(domain, unmatchedCategory, emotion, targetSituation);
            UsersLinku direct = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuDirect, emotion).situation(targetSituation).build());

            // category만 일치: situation은 null(직접 일치 불가능), category는 매핑된 카테고리.
            // 동시에 "situation=null인 저장 링크도 결과에서 안 빠지는지" 회귀 검증을 겸한다.
            Linku linkuCategoryOnly = createAndSaveLinku(domain, matchedCategory, emotion, otherSituation);
            UsersLinku categoryOnly = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuCategoryOnly, emotion).situation(null).build());

            // 매칭 없음: situation도 다르고 category도 안 맞음.
            Linku linkuNone = createAndSaveLinku(domain, unmatchedCategory, emotion, otherSituation);
            UsersLinku none = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuNone, emotion).situation(otherSituation).build());

            List<UsersLinku> result = usersLinkuRepository.findHomeRecommendCandidates(
                    user.getId(), emotion.getEmotionId(), targetSituation.getId(), List.of(matchedCategory.getCategoryId()),
                    LocalDateTime.now(), null, null, 0, 10);

            assertThat(result).hasSize(3); // situation=null인 categoryOnly도 빠지지 않는다
            assertThat(result).extracting(UsersLinku::getUserLinkuId)
                    .containsExactly(direct.getUserLinkuId(), categoryOnly.getUserLinkuId(), none.getUserLinkuId());
        }
    }

    @Nested
    @DisplayName("PersonalEngagement / Popularity")
    class EngagementAndPopularityOrdering {

        @Test
        @DisplayName("viewCount/lastViewedAt/totalViewCount가 높을수록 상위로 정렬된다")
        void higherEngagementAndPopularityRanksHigher() {
            Users user = userRepository.save(createUser("home-reco-engagement"));
            Domain domain = domainRepository.save(createDomain("engagement-test"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("카테고리", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation("상황"));

            LocalDateTime now = LocalDateTime.now();

            // 인기/재방문 신호가 전부 낮은 링크
            Linku linkuLow = createAndSaveLinku(domain, category, emotion, situation, 0L);
            UsersLinku low = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuLow, emotion).situation(situation)
                            .viewCount(0).lastViewedAt(now.minusDays(60)).build());

            // 인기/재방문 신호가 전부 높은 링크
            Linku linkuHigh = createAndSaveLinku(domain, category, emotion, situation, 5000L);
            UsersLinku high = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuHigh, emotion).situation(situation)
                            .viewCount(50).lastViewedAt(now.minusHours(1)).build());

            List<UsersLinku> result = usersLinkuRepository.findHomeRecommendCandidates(
                    user.getId(), emotion.getEmotionId(), situation.getId(), List.of(category.getCategoryId()),
                    now, null, null, 0, 10);

            assertThat(result).extracting(UsersLinku::getUserLinkuId)
                    .containsExactly(high.getUserLinkuId(), low.getUserLinkuId());
        }
    }

    private Users createUser(String nickName) {
        return Users.builder()
                .nickName(nickName)
                .password("password")
                .role(Role.USER)
                .build();
    }

    private Domain createDomain(String tail) {
        return Domain.builder()
                .domainTail(tail)
                .name(tail)
                .imageUrl("https://img.example.com/" + tail + ".png")
                .build();
    }

    private Fcolor createFcolor() {
        return Fcolor.builder()
                .colorName("BLUE")
                .colorCode1("#E3F2FD")
                .colorCode2("#90CAF9")
                .colorCode3("#42A5F5")
                .colorCode4("#1E88E5")
                .build();
    }

    private Category createCategory(String name, Fcolor fcolor) {
        return Category.builder().categoryName(name).fcolor(fcolor).build();
    }

    private Emotion createEmotion() {
        return Emotion.builder().name("테스트감정").build();
    }

    private Situation createSituation(String name) {
        return Situation.builder().name(name).build();
    }

    private Linku createAndSaveLinku(Domain domain, Category category, Emotion emotion, Situation situation) {
        return createAndSaveLinku(domain, category, emotion, situation, 0L);
    }

    private Linku createAndSaveLinku(Domain domain, Category category, Emotion emotion, Situation situation, long totalViewCount) {
        Linku linku = Linku.builder()
                .title("테스트 링크 " + System.nanoTime())
                .linkuUrl("https://example.com/" + System.nanoTime())
                .category(category)
                .domain(domain)
                .emotion(emotion)
                .situation(situation)
                .totalViewCount(totalViewCount)
                .build();
        return linkuRepository.save(linku);
    }

    private UsersLinku.UsersLinkuBuilder baseUsersLinku(Users user, Linku linku, Emotion emotion) {
        return UsersLinku.builder()
                .user(user)
                .linku(linku)
                .emotion(emotion)
                .title(linku.getTitle());
    }
}
