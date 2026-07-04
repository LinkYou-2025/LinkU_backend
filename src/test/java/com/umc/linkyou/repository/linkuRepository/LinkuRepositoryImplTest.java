package com.umc.linkyou.repository.linkuRepository;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.categoryRepository.FcolorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.config.TestExternalConfig;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
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

    @Nested
    @DisplayName("검색어 찾기")
    class FindUserSavedSuggestions {

        @Nested
        @DisplayName("성공 케이스")
        class SuccessCase {

            @Test
            @DisplayName("prefix 검색이 정상 동작한다")
            void prefixSearch() {
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

                Linku java1 = linkuRepository.save(createLinku("Java Guide", "link1", category, domain, emotion));
                Linku java2 = linkuRepository.save(createLinku("Java Spring", "link2", category, domain, emotion));
                Linku python = linkuRepository.save(createLinku("Python Basics", "link3", category, domain, emotion));

                usersLinkuRepository.save(createUsersLinku(user, java1, emotion));
                usersLinkuRepository.save(createUsersLinku(user, java2, emotion));
                usersLinkuRepository.save(createUsersLinku(user, python, emotion));
                usersLinkuRepository.save(createUsersLinku(otherUser, java1, emotion));

                List<LinkuSearchSuggestionResponse> result =
                        linkuRepository.findUserSavedSuggestions(user.getId(), "Ja");

                assertThat(result).hasSize(2);
                assertThat(result).extracting(LinkuSearchSuggestionResponse::title)
                        .containsExactly("Java Guide", "Java Spring");
            }
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
            void success() {
                Domain domain = domainRepository.save(
                        createDomain("google.com", "구글", "https://image.com/a.png")
                );

                Fcolor fcolor = fcolorRepository.save(createFcolor());

                Category category = categoryRepository.save(
                        createCategory("개발", fcolor)
                );

                Emotion emotion = emotionRepository.save(createEmotion());
                linkuRepository.save(createLinku("Java Guide", "abc123", category, domain, emotion));
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
            void notFound() {
                Optional<Linku> result = linkuRepository.findByLinku("not-exists");

                assertThat(result).isEmpty();
            }
        }
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

    private Linku createLinku(String title, String linku, Category category, Domain domain, Emotion emotion) {
        return Linku.builder()
                .title(title)
                .linkuUrl(linku)
                .category(category)
                .domain(domain)
                .emotion(emotion)
                .build();
    }

    private UsersLinku createUsersLinku(Users user, Linku linku, Emotion emotion) {
        return UsersLinku.builder()
                .user(user)
                .linku(linku)
                .emotion(emotion)
                .build();
    }
}
