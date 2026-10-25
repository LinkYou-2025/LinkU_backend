package com.umc.linkyou.repository.linkuRepository;

import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.mapping.LinkuKeyword;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.categoryRepository.FcolorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordRepository;
import com.umc.linkyou.repository.mapping.LinkuKeywordRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.config.TestExternalConfig;
import com.umc.linkyou.web.dto.keyword.KeywordLinkuItemDTO;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
@DisplayName("LinkuRepositoryImpl 테스트")
class LinkuRepositoryImplTest {

    @Autowired
    private LinkuRepository linkuRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UsersLinkuRepository usersLinkuRepository;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FcolorRepository fcolorRepository;

    @Autowired
    private EmotionRepository emotionRepository;

    @Autowired
    private SituationRepository situationRepository;

    @Autowired
    private KeywordRepository keywordRepository;

    @Autowired
    private LinkuKeywordRepository linkuKeywordRepository;

    @Nested
    @DisplayName("링크 검색 (커서 페이징)")
    class SearchUserLinks {

        @Nested
        @DisplayName("성공 케이스")
        class SuccessCase {

            @Test
            @DisplayName("본인이 저장한 링크만 최신 저장 순으로 검색된다")
            void 본인이_저장한_링크만_최신_저장_순으로_검색된다() {
                Users user = userRepository.save(createUser("user1"));
                Users otherUser = userRepository.save(createUser("user2"));

                Domain domain = domainRepository.save(
                        createDomain("google.com", "구글", "https://image.com/a.png")
                );

                Fcolor fcolor = fcolorRepository.save(createFcolor());

                Category category = categoryRepository.save(
                        createCategory("개발", fcolor)
                );

                Emotion emotion = emotionRepository.save(createEmotion());
                Situation situation = situationRepository.save(createSituation());

                Linku java1 = linkuRepository.save(
                        createLinku("Java Guide", "link1", category, domain, emotion, situation)
                );
                Linku java2 = linkuRepository.save(
                        createLinku("Java Spring", "link2", category, domain, emotion, situation)
                );
                Linku python = linkuRepository.save(
                        createLinku("Python Basics", "link3", category, domain, emotion, situation)
                );

                usersLinkuRepository.save(createUsersLinku(user, java1, emotion));
                usersLinkuRepository.save(createUsersLinku(user, java2, emotion));
                usersLinkuRepository.save(createUsersLinku(user, python, emotion));
                usersLinkuRepository.save(createUsersLinku(otherUser, java1, emotion));

                List<LinkuSearchResponseDTO.LinkuSearchItemDTO> result =
                        linkuRepository.searchUserLinks(user.getId(), "Ja", null, 10);

                assertThat(result).hasSize(2);
                assertThat(result).extracting(LinkuSearchResponseDTO.LinkuSearchItemDTO::title)
                        .containsExactly("Java Spring", "Java Guide"); // 최신 저장 순
                assertThat(result).allSatisfy(item -> assertThat(item.tags()).isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("검색어 자동완성 (findQuickByKeyword)")
    class FindQuickByKeyword {

        @Test
        @DisplayName("제목에 키워드가 포함된 후보를 최대 3개 반환한다")
        void 제목에_키워드가_포함된_후보를_최대_3개_반환한다() {
            Users user = userRepository.save(createUser("autocomplete_u1"));
            Domain domain = domainRepository.save(createDomain("google.com", "구글", "https://img.com/a.png"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("개발", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation());

            for (int i = 1; i <= 5; i++) {
                Linku l = linkuRepository.save(createLinku("Java 강의 " + i, "linkQ" + i, category, domain, emotion, situation));
                usersLinkuRepository.save(createUsersLinku(user, l, emotion));
            }

            List<LinkuQuickSearchResponseDTO> result = linkuRepository.findQuickByKeyword(user.getId(), "Java");

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("사용자 지정 제목(ul.title)으로 자동완성이 동작한다")
        void 사용자_지정_제목으로_자동완성이_동작한다() {
            Users user = userRepository.save(createUser("autocomplete_u2"));
            Domain domain = domainRepository.save(createDomain("google.com", "구글", "https://img.com/b.png"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("개발", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation());

            Linku l = linkuRepository.save(createLinku("원본 크롤링 제목", "linkQX", category, domain, emotion, situation));
            usersLinkuRepository.save(createUsersLinkuWithTitle(user, l, emotion, "커스텀 자바 가이드"));

            List<LinkuQuickSearchResponseDTO> result = linkuRepository.findQuickByKeyword(user.getId(), "자바");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("커스텀 자바 가이드");
        }

        @Test
        @DisplayName("다른 사용자의 링크는 포함되지 않는다")
        void 다른_사용자의_링크는_포함되지_않는다() {
            Users user = userRepository.save(createUser("autocomplete_u3"));
            Users other = userRepository.save(createUser("autocomplete_u4"));
            Domain domain = domainRepository.save(createDomain("google.com", "구글", "https://img.com/c.png"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("개발", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation());

            Linku l = linkuRepository.save(createLinku("Java Tips", "linkQY", category, domain, emotion, situation));
            usersLinkuRepository.save(createUsersLinku(other, l, emotion));

            List<LinkuQuickSearchResponseDTO> result = linkuRepository.findQuickByKeyword(user.getId(), "Java");

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByLinku")
    class FindByLinku {

        @Nested
        @DisplayName("성공 케이스")
        class SuccessCase {

            @Test
            @DisplayName("linku 값으로 정상 조회된다")
            void linku_값으로_정상_조회된다() {
                Domain domain = domainRepository.save(
                        createDomain("google.com", "구글", "https://image.com/a.png")
                );

                Fcolor fcolor = fcolorRepository.save(createFcolor());

                Category category = categoryRepository.save(
                        createCategory("개발", fcolor)
                );

                Emotion emotion = emotionRepository.save(createEmotion());
                Situation situation = situationRepository.save(createSituation());

                linkuRepository.save(
                        createLinku("Java Guide", "abc123", category, domain, emotion, situation)
                );

                Optional<Linku> result = linkuRepository.findByLinku("abc123");

                assertThat(result).isPresent();
                assertThat(result.get().getTitle()).isEqualTo("Java Guide");
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class FailureCase {

            @Test
            @DisplayName("존재하지 않으면 empty를 반환한다")
            void 존재하지_않으면_empty를_반환한다() {
                Optional<Linku> result = linkuRepository.findByLinku("not-exists");

                assertThat(result).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("키워드 정확 일치 검색 (findUserLinksByExactKeyword)")
    class FindUserLinksByExactKeyword {

        @Test
        @DisplayName("정확히 일치하는 키워드로 저장한 본인 링크가 조회된다")
        void 정확히_일치하는_키워드로_저장한_본인_링크가_조회된다() {
            Users user = userRepository.save(createUser("exactkw_u1"));
            Domain domain = domainRepository.save(createDomain("google.com", "구글", "https://img.com/a.png"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("개발", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation());
            Keyword keyword = keywordRepository.save(createKeyword("자바"));

            Linku linku = linkuRepository.save(createLinku("Java Guide", "exact-link1", category, domain, emotion, situation));
            linkuKeywordRepository.save(createLinkuKeyword(linku, keyword));
            usersLinkuRepository.save(createUsersLinku(user, linku, emotion));

            List<KeywordLinkuItemDTO> result = linkuRepository.findUserLinksByExactKeyword(user.getId(), "자바");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Java Guide");
            assertThat(result.get(0).categories()).containsExactly("개발", "기쁨");
        }

        @Test
        @DisplayName("AI 아티클이 생성된 링크는 aiArticleExists가 true로 반환된다")
        void AI_아티클이_생성된_링크는_aiArticleExists가_true로_반환된다() {
            Users user = userRepository.save(createUser("exactkw_u2"));
            Domain domain = domainRepository.save(createDomain("google.com", "구글", "https://img.com/b.png"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("개발", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation());
            Keyword keyword = keywordRepository.save(createKeyword("스프링"));

            Linku linku = linkuRepository.save(createLinku("Spring Guide", "exact-link2", category, domain, emotion, situation));
            linkuKeywordRepository.save(createLinkuKeyword(linku, keyword));
            UsersLinku usersLinku = createUsersLinku(user, linku, emotion);
            usersLinku.markAiExist(true);
            usersLinkuRepository.save(usersLinku);

            List<KeywordLinkuItemDTO> result = linkuRepository.findUserLinksByExactKeyword(user.getId(), "스프링");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).aiArticleExists()).isTrue();
        }

        @Test
        @DisplayName("AI 아티클이 생성되지 않은 링크는 aiArticleExists가 false로 반환된다")
        void AI_아티클이_생성되지_않은_링크는_aiArticleExists가_false로_반환된다() {
            Users user = userRepository.save(createUser("exactkw_u3"));
            Domain domain = domainRepository.save(createDomain("google.com", "구글", "https://img.com/c.png"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("개발", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation());
            Keyword keyword = keywordRepository.save(createKeyword("파이썬"));

            Linku linku = linkuRepository.save(createLinku("Python Guide", "exact-link3", category, domain, emotion, situation));
            linkuKeywordRepository.save(createLinkuKeyword(linku, keyword));
            usersLinkuRepository.save(createUsersLinku(user, linku, emotion));

            List<KeywordLinkuItemDTO> result = linkuRepository.findUserLinksByExactKeyword(user.getId(), "파이썬");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).aiArticleExists()).isFalse();
        }

        @Test
        @DisplayName("키워드가 부분적으로만 일치하면 조회되지 않는다")
        void 키워드가_부분적으로만_일치하면_조회되지_않는다() {
            Users user = userRepository.save(createUser("exactkw_u4"));
            Domain domain = domainRepository.save(createDomain("google.com", "구글", "https://img.com/d.png"));
            Fcolor fcolor = fcolorRepository.save(createFcolor());
            Category category = categoryRepository.save(createCategory("개발", fcolor));
            Emotion emotion = emotionRepository.save(createEmotion());
            Situation situation = situationRepository.save(createSituation());
            Keyword keyword = keywordRepository.save(createKeyword("자바스크립트"));

            Linku linku = linkuRepository.save(createLinku("JS Guide", "exact-link4", category, domain, emotion, situation));
            linkuKeywordRepository.save(createLinkuKeyword(linku, keyword));
            usersLinkuRepository.save(createUsersLinku(user, linku, emotion));

            List<KeywordLinkuItemDTO> result = linkuRepository.findUserLinksByExactKeyword(user.getId(), "자바");

            assertThat(result).isEmpty();
        }
    }

    @Test
    @DisplayName("findByLinku 조회 후 domain 정보 접근이 가능하다")
    void findByLinku_조회_후_domain_정보_접근이_가능하다() {
        Domain domain = domainRepository.save(
                createDomain("google.com", "구글", "https://image.com/a.png")
        );

        Fcolor fcolor = fcolorRepository.save(createFcolor());

        Category category = categoryRepository.save(
                createCategory("개발", fcolor)
        );

        Emotion emotion = emotionRepository.save(createEmotion());
        Situation situation = situationRepository.save(createSituation());

        linkuRepository.save(
                createLinku("Java Guide", "abc123", category, domain, emotion, situation)
        );

        Optional<Linku> result = linkuRepository.findByLinku("abc123");

        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("Java Guide");
        assertThat(result.get().getDomain()).isNotNull();
        assertThat(result.get().getDomain().getName()).isEqualTo("구글");
        assertThat(result.get().getDomain().getImageUrl()).isEqualTo("https://image.com/a.png");
    }

    private Users createUser(String nickName) {
        return Users.builder()
                .nickName(nickName)
                .password("password")
                .role(Role.USER)
                .build();
    }

    private Domain createDomain(String domainTail, String name, String imageUrl) {
        return Domain.builder()
                .domainTail(domainTail)
                .name(name)
                .imageUrl(imageUrl)
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

    private Category createCategory(String categoryName, Fcolor fcolor) {
        return Category.builder()
                .categoryName(categoryName)
                .fcolor(fcolor)
                .build();
    }

    private Emotion createEmotion() {
        return Emotion.builder()
                .name("기쁨")
                .build();
    }

    private Situation createSituation() {
        return Situation.builder()
                .name("일상")
                .build();
    }

    private Linku createLinku(
            String title,
            String linku,
            Category category,
            Domain domain,
            Emotion emotion,
            Situation situation
    ) {
        return Linku.builder()
                .title(title)
                .linkuUrl(linku)
                .category(category)
                .domain(domain)
                .emotion(emotion)
                .situation(situation)
                .build();
    }

    private UsersLinku createUsersLinku(Users user, Linku linku, Emotion emotion) {
        return UsersLinku.builder()
                .user(user)
                .linku(linku)
                .emotion(emotion)
                .build();
    }

    private UsersLinku createUsersLinkuWithTitle(Users user, Linku linku, Emotion emotion, String customTitle) {
        return UsersLinku.builder()
                .user(user)
                .linku(linku)
                .emotion(emotion)
                .title(customTitle)
                .build();
    }

    private Keyword createKeyword(String name) {
        return Keyword.builder()
                .name(name)
                .build();
    }

    private LinkuKeyword createLinkuKeyword(Linku linku, Keyword keyword) {
        return LinkuKeyword.builder()
                .linku(linku)
                .keyword(keyword)
                .build();
    }
}
