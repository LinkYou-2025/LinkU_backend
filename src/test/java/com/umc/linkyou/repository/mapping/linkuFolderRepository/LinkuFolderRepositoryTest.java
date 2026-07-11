package com.umc.linkyou.repository.mapping.linkuFolderRepository;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.EmotionRepository;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.UserLinkuRepository.UsersLinkuRepository;
import com.umc.linkyou.repository.categoryRepository.FcolorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.classification.SituationRepository;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.config.TestExternalConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
@DisplayName("LinkuFolderRepository 테스트")
class LinkuFolderRepositoryTest {

    @Autowired private LinkuFolderRepository linkuFolderRepository;
    @Autowired private LinkuRepository linkuRepository;
    @Autowired private UsersLinkuRepository usersLinkuRepository;
    @Autowired private FolderRepository folderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DomainRepository domainRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private FcolorRepository fcolorRepository;
    @Autowired private EmotionRepository emotionRepository;
    @Autowired private SituationRepository situationRepository;

    @PersistenceContext
    private EntityManager em;

    @Nested
    @DisplayName("findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc")
    class FindFirstByUsersLinkuUserLinkuId {

        @Nested
        @DisplayName("성공 케이스")
        class SuccessCase {

            @Test
            @DisplayName("한 링크에 매핑이 여러 개면 가장 최근에 생성된(linkuFolderId가 가장 큰) 매핑을 반환한다")
            void 매핑이_여러개면_가장_최근_매핑을_반환한다() {
                // given
                Category category = saveCategory("기술");
                Users user = userRepository.save(createUser("user1"));
                UsersLinku usersLinku = usersLinkuRepository.save(createUsersLinku(user, category));

                Folder firstFolder = folderRepository.save(createFolder("첫 폴더", category));
                Folder secondFolder = folderRepository.save(createFolder("둘째 폴더", category));

                linkuFolderRepository.save(createLinkuFolder(firstFolder, usersLinku));
                LinkuFolder latest = linkuFolderRepository.save(createLinkuFolder(secondFolder, usersLinku));
                em.flush();
                em.clear();

                // when
                Optional<LinkuFolder> result = linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId());

                // then
                assertThat(result).isPresent();
                assertThat(result.get().getLinkuFolderId()).isEqualTo(latest.getLinkuFolderId());
                assertThat(result.get().getFolder().getFolderId()).isEqualTo(secondFolder.getFolderId());
            }

            @Test
            @DisplayName("폴더 이동(updateFolder) 후 저장하면 변경된 폴더 정보가 조회에 반영된다")
            void 폴더_이동_후_변경된_폴더가_조회에_반영된다() {
                // given
                Category category = saveCategory("기술");
                Users user = userRepository.save(createUser("user2"));
                UsersLinku usersLinku = usersLinkuRepository.save(createUsersLinku(user, category));

                Folder oldFolder = folderRepository.save(createFolder("옛 폴더", category));
                Folder newFolder = folderRepository.save(createFolder("새 폴더", category));

                LinkuFolder linkuFolder = linkuFolderRepository.save(createLinkuFolder(oldFolder, usersLinku));
                em.flush();
                em.clear();

                // when: 서비스 로직과 동일하게 폴더만 교체 후 저장
                LinkuFolder managed = linkuFolderRepository.findById(linkuFolder.getLinkuFolderId()).orElseThrow();
                managed.updateFolder(newFolder);
                linkuFolderRepository.save(managed);
                em.flush();
                em.clear();

                // then
                LinkuFolder reloaded = linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId())
                        .orElseThrow();
                assertThat(reloaded.getFolder().getFolderId()).isEqualTo(newFolder.getFolderId());
                assertThat(reloaded.getFolder().getFolderName()).isEqualTo("새 폴더");
            }

            @Test
            @DisplayName("소분류(하위) 폴더로 이동해도 정상적으로 반영된다")
            void 소분류_폴더로_이동해도_정상적으로_반영된다() {
                // given: 중분류(루트) 폴더 아래에 소분류(하위) 폴더를 만든다
                Category category = saveCategory("기술");
                Users user = userRepository.save(createUser("user3"));
                UsersLinku usersLinku = usersLinkuRepository.save(createUsersLinku(user, category));

                Folder rootFolder = folderRepository.save(createFolder("중분류", category));
                Folder subFolder = folderRepository.save(Folder.builder()
                        .folderName("소분류")
                        .category(category)
                        .parentFolder(rootFolder)
                        .build());

                LinkuFolder linkuFolder = linkuFolderRepository.save(createLinkuFolder(rootFolder, usersLinku));
                em.flush();
                em.clear();

                // when
                LinkuFolder managed = linkuFolderRepository.findById(linkuFolder.getLinkuFolderId()).orElseThrow();
                managed.updateFolder(subFolder);
                linkuFolderRepository.save(managed);
                em.flush();
                em.clear();

                // then
                LinkuFolder reloaded = linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(usersLinku.getUserLinkuId())
                        .orElseThrow();
                assertThat(reloaded.getFolder().getFolderId()).isEqualTo(subFolder.getFolderId());
                assertThat(reloaded.getFolder().getParentFolder().getFolderId()).isEqualTo(rootFolder.getFolderId());
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class FailureCase {

            @Test
            @DisplayName("해당 유저링크의 폴더 매핑이 없으면 빈 값을 반환한다")
            void 매핑이_없으면_빈값을_반환한다() {
                // when
                Optional<LinkuFolder> result = linkuFolderRepository
                        .findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(999999L);

                // then
                assertThat(result).isEmpty();
            }
        }
    }

    private Category saveCategory(String name) {
        Fcolor fcolor = fcolorRepository.save(Fcolor.builder()
                .colorName("BLUE")
                .colorCode1("#E3F2FD")
                .colorCode2("#90CAF9")
                .colorCode3("#42A5F5")
                .colorCode4("#1E88E5")
                .build());
        return categoryRepository.save(Category.builder()
                .categoryName(name)
                .fcolor(fcolor)
                .build());
    }

    private Users createUser(String nickName) {
        return Users.builder()
                .nickName(nickName)
                .password("password")
                .role(Role.USER)
                .build();
    }

    private Folder createFolder(String name, Category category) {
        return Folder.builder()
                .folderName(name)
                .category(category)
                .build();
    }

    private UsersLinku createUsersLinku(Users user, Category category) {
        Domain domain = domainRepository.save(Domain.builder()
                .domainTail("example.com")
                .name("example")
                .imageUrl("https://image.com/a.png")
                .build());
        Emotion emotion = emotionRepository.save(Emotion.builder().name("기쁨").build());
        Situation situation = situationRepository.save(Situation.builder().name("일상").build());

        Linku linku = linkuRepository.save(Linku.builder()
                .title("테스트 링크")
                .linkuUrl("https://example.com/" + System.nanoTime())
                .category(category)
                .domain(domain)
                .emotion(emotion)
                .situation(situation)
                .build());

        return UsersLinku.builder()
                .user(user)
                .linku(linku)
                .emotion(emotion)
                .situation(situation)
                .build();
    }

    private LinkuFolder createLinkuFolder(Folder folder, UsersLinku usersLinku) {
        return LinkuFolder.builder()
                .folder(folder)
                .usersLinku(usersLinku)
                .build();
    }
}
