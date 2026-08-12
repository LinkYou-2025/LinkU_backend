package com.umc.linkyou.repository.UserLinkuRepository;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.domain.mapping.LinkuKeyword;
import com.umc.linkyou.domain.recommend.UserProfileKeyword;
import com.umc.linkyou.repository.dto.RankedUsersLinku;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.categoryRepository.FcolorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordRepository;
import com.umc.linkyou.repository.mapping.LinkuKeywordRepository;
import com.umc.linkyou.repository.recommend.UserProfileKeywordRepository;
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
 * UsersLinkuRepositoryImpl 홈화면 추천 조회(findNormalRecommendCandidates) 통합 테스트.
 *
 * TextMatch용 profileTsqueryText/profileText는 null로 넘긴다 — 둘 다 null이면 nativeTextExpression이
 * similarity()/ts_rank_cd를 호출하지 않아 pg_trgm 확장 설치 여부와 무관하게 KeywordMatch 및 커서 동작을
 * 검증할 수 있다.
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
@DisplayName("UsersLinkuRepositoryImpl 홈화면 추천 조회 테스트")
class UsersLinkuRepositoryImplTest {

    @Autowired private UsersLinkuRepository usersLinkuRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DomainRepository domainRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private FcolorRepository fcolorRepository;
    @Autowired private EmotionRepository emotionRepository;
    @Autowired private SituationRepository situationRepository;
    @Autowired private LinkuRepository linkuRepository;
    @Autowired private KeywordRepository keywordRepository;
    @Autowired private LinkuKeywordRepository linkuKeywordRepository;
    @Autowired private UserProfileKeywordRepository userProfileKeywordRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    // BaseEntity.createdAt은 @CreatedDate(updatable=false)라 JPA로는 저장 후 값을 바꿀 수 없어서,
    // "저장한 지 오래된" 상태를 재현하려면 raw SQL로 직접 덮어써야 한다
    // (DeleteOldAlarmBatchIntegrationTest#saveAlarm과 같은 패턴).
    private void overrideCreatedAt(Long userLinkuId, LocalDateTime createdAt) {
        jdbcTemplate.update("UPDATE users_linkus SET created_at = ? WHERE user_linku_id = ?", createdAt, userLinkuId);
    }

    @Nested
    @DisplayName("PersonalEngagement staleness")
    class PersonalEngagementStaleness {

        @Test
        @DisplayName("오래 안 본/안 만든 후보일수록 PersonalEngagement staleness가 높아 점수가 더 높다")
        void 오래_안_본_안_만든_후보일수록_점수가_더_높다() {
            Users user = userRepository.save(createUser("home-reco-staleness"));
            Domain domain = domainRepository.save(createDomain("staleness-test"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("카테고리", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation("상황"));

            LocalDateTime now = LocalDateTime.now();

            // 한 번도 안 봄(lastViewedAt=null) + 저장한 지 30일 지남 → staleness 높음
            Linku linkuNeverViewedOld = createAndSaveLinku(domain, category, emotion, situation);
            UsersLinku neverViewedOld = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuNeverViewedOld, emotion).situation(situation).build());
            overrideCreatedAt(neverViewedOld.getUserLinkuId(), now.minusDays(30));

            // 한 번도 안 봄 + 방금 저장(기본 createdAt=now) → staleness 낮음
            Linku linkuNeverViewedRecent = createAndSaveLinku(domain, category, emotion, situation);
            UsersLinku neverViewedRecent = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuNeverViewedRecent, emotion).situation(situation).build());

            // 예전 novelty 하드 필터와 달리 지금은 둘 다(과거엔 하나는 제외, 하나는 포함) 같은 랭킹 안에
            // 남아 있으면서, 오래된 쪽이 staleness가 높아 위로 온다.
            List<RankedUsersLinku> result = usersLinkuRepository.findNormalRecommendCandidates(
                    user.getId(), emotion.getEmotionId(), situation.getId(), List.of(category.getCategoryId()),
                    now, null, null, null, null, 10);

            assertThat(result).extracting(RankedUsersLinku::userLinkuId)
                    .containsExactly(neverViewedOld.getUserLinkuId(), neverViewedRecent.getUserLinkuId());
        }
    }

    @Nested
    @DisplayName("점수 커서")
    class ScoreCursor {

        @Test
        @DisplayName("키워드 10개와 커서가 있을 시 다음 후보를 반환한다")
        void 키워드_10개와_커서가_있을_시_다음_후보를_반환한다() {
            Users user = userRepository.save(createUser("home-reco-cursor"));
            Domain domain = domainRepository.save(createDomain("cursor-test"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("카테고리", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation("상황"));

            List<Keyword> keywords = keywordRepository.saveAll(
                    java.util.stream.IntStream.rangeClosed(1, 10)
                            .mapToObj(keywordId -> Keyword.builder().name("cursor-keyword-" + keywordId).build())
                            .toList());
            for (int keywordIndex = 0; keywordIndex < keywords.size(); keywordIndex++) {
                userProfileKeywordRepository.save(UserProfileKeyword.builder()
                        .userId(user.getId())
                        .keywordId(keywords.get(keywordIndex).getId())
                        .weight(keywordIndex + 1)
                        .build());
            }

            LocalDateTime now = LocalDateTime.now();
            Linku lowKeywordLinku = createAndSaveLinku(domain, category, emotion, situation);
            Linku highKeywordLinku = createAndSaveLinku(domain, category, emotion, situation);
            Linku noKeywordLinku = createAndSaveLinku(domain, category, emotion, situation);
            linkuKeywordRepository.saveAll(List.of(
                    LinkuKeyword.builder().linku(lowKeywordLinku).keyword(keywords.get(8)).build(),
                    LinkuKeyword.builder().linku(lowKeywordLinku).keyword(keywords.get(9)).build()));
            linkuKeywordRepository.saveAll(keywords.stream()
                    .map(keyword -> LinkuKeyword.builder().linku(highKeywordLinku).keyword(keyword).build())
                    .toList());

            UsersLinku lowKeywordCandidate = usersLinkuRepository.save(baseUsersLinku(user, lowKeywordLinku, emotion)
                    .situation(situation)
                    .lastViewedAt(now.minusDays(1))
                    .build());
            UsersLinku highKeywordCandidate = usersLinkuRepository.save(baseUsersLinku(user, highKeywordLinku, emotion)
                    .situation(situation)
                    .lastViewedAt(now.minusDays(1))
                    .build());
            usersLinkuRepository.save(baseUsersLinku(user, noKeywordLinku, emotion)
                    .situation(situation)
                    .lastViewedAt(now.minusDays(1))
                    .build());

            List<RankedUsersLinku> firstPage = usersLinkuRepository.findNormalRecommendCandidates(
                    user.getId(), emotion.getEmotionId(), situation.getId(), List.of(category.getCategoryId()),
                    now, null, null, null, null, 1);
            assertThat(firstPage).hasSize(1);
            assertThat(firstPage.get(0).userLinkuId()).isEqualTo(highKeywordCandidate.getUserLinkuId());
            RankedUsersLinku cursor = firstPage.get(0);

            List<RankedUsersLinku> secondPage = usersLinkuRepository.findNormalRecommendCandidates(
                    user.getId(), emotion.getEmotionId(), situation.getId(), List.of(category.getCategoryId()),
                    now, null, null, cursor.scoreBucket(), cursor.userLinkuId(), 1);

            assertThat(secondPage).hasSize(1);
            assertThat(secondPage.get(0).userLinkuId()).isEqualTo(lowKeywordCandidate.getUserLinkuId());
            assertThat(cursor.scoreBucket()).isGreaterThan(secondPage.get(0).scoreBucket());
        }
    }

    @Nested
    @DisplayName("SituationMatch")
    class SituationMatchOrdering {

        @Test
        @DisplayName("직접 일치 > category 매핑만 일치 > 매칭 없음 순으로 정렬되고, situation=null인 후보도 결과에서 빠지지 않는다")
        void 직접_일치_category_매핑_매칭없음_순으로_정렬된다() {
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

            // 셋 다 방금 저장(lastViewedAt=null, createdAt=now)이라 staleness가 거의 동일해서
            // PersonalEngagement 차이는 무시할 만하고, SituationMatch/CategoryMatch 차이로만 정렬된다.
            List<RankedUsersLinku> result = usersLinkuRepository.findNormalRecommendCandidates(
                    user.getId(), emotion.getEmotionId(), targetSituation.getId(), List.of(matchedCategory.getCategoryId()),
                    LocalDateTime.now(), null, null, null, null, 10);

            assertThat(result).hasSize(3); // situation=null인 categoryOnly도 빠지지 않는다
            assertThat(result).extracting(RankedUsersLinku::userLinkuId)
                    .containsExactly(direct.getUserLinkuId(), categoryOnly.getUserLinkuId(), none.getUserLinkuId());
        }
    }

    @Nested
    @DisplayName("PersonalEngagement / Popularity")
    class EngagementAndPopularityOrdering {

        @Test
        @DisplayName("viewCount/lastViewedAt/totalViewCount가 높을수록 상위로 정렬된다")
        void engagement와_popularity가_높을수록_상위로_정렬된다() {
            Users user = userRepository.save(createUser("home-reco-engagement"));
            Domain domain = domainRepository.save(createDomain("engagement-test"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("카테고리", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation("상황"));

            LocalDateTime now = LocalDateTime.now();

            // 인기/재방문 빈도(viewCount)·totalViewCount가 낮은 링크. staleness는 오히려 낮은(=최근에 봄)
            // 쪽이라 PersonalEngagement 항에서도 불리하다 — viewCount/popularity/staleness 모두 high가 이긴다.
            Linku linkuLow = createAndSaveLinku(domain, category, emotion, situation, 0L);
            UsersLinku low = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuLow, emotion).situation(situation)
                            .viewCount(0).lastViewedAt(now.minusDays(10)).build());

            // 인기/재방문 빈도·totalViewCount가 전부 높은 링크
            Linku linkuHigh = createAndSaveLinku(domain, category, emotion, situation, 5000L);
            UsersLinku high = usersLinkuRepository.save(
                    baseUsersLinku(user, linkuHigh, emotion).situation(situation)
                            .viewCount(50).lastViewedAt(now.minusHours(1)).build());

            List<RankedUsersLinku> result = usersLinkuRepository.findNormalRecommendCandidates(
                    user.getId(), emotion.getEmotionId(), situation.getId(), List.of(category.getCategoryId()),
                    now, null, null, null, null, 10);

            assertThat(result).extracting(RankedUsersLinku::userLinkuId)
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
