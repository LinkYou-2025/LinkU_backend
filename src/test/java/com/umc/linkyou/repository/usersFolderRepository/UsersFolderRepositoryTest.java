package com.umc.linkyou.repository.usersFolderRepository;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.categoryRepository.FcolorRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.support.config.TestExternalConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Import(TestExternalConfig.class)
@DisplayName("UsersFolderRepository 테스트")
class UsersFolderRepositoryTest {

    @Autowired private UsersFolderRepository usersFolderRepository;
    @Autowired private FolderRepository folderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private FcolorRepository fcolorRepository;

    @Nested
    @DisplayName("existsFolderOwnerOrWriter")
    class ExistsFolderOwnerOrWriter {

        @Nested
        @DisplayName("성공 케이스")
        class SuccessCase {

            @Test
            @DisplayName("OWNER 권한이면 true를 반환한다")
            void OWNER_권한이면_true를_반환한다() {
                // given
                Category category = saveCategory("기술");
                Users user = userRepository.save(createUser("owner"));
                Folder folder = folderRepository.save(createFolder("내 폴더", category));
                usersFolderRepository.save(createUsersFolder(user, folder, PermissionType.OWNER));

                // when
                boolean result = usersFolderRepository.existsFolderOwnerOrWriter(user.getId(), folder.getFolderId());

                // then
                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("WRITER 권한이면 true를 반환한다")
            void WRITER_권한이면_true를_반환한다() {
                // given
                Category category = saveCategory("기술");
                Users owner = userRepository.save(createUser("owner2"));
                Users writer = userRepository.save(createUser("writer"));
                Folder folder = folderRepository.save(createFolder("공유 폴더", category));
                usersFolderRepository.save(createUsersFolder(owner, folder, PermissionType.OWNER));
                usersFolderRepository.save(createUsersFolder(writer, folder, PermissionType.WRITER));

                // when
                boolean result = usersFolderRepository.existsFolderOwnerOrWriter(writer.getId(), folder.getFolderId());

                // then
                assertThat(result).isTrue();
            }

            @Test
            @DisplayName("대상이 소분류(하위) 폴더여도 그 폴더 자체의 소유자면 true를 반환한다")
            void 소분류_폴더의_소유자면_true를_반환한다() {
                // given: 중분류(루트) 폴더 아래에 소분류(하위) 폴더를 만들고, 하위 폴더에 대해서만 소유권을 등록
                Category category = saveCategory("기술");
                Users user = userRepository.save(createUser("subOwner"));
                Folder rootFolder = folderRepository.save(createFolder("중분류", category));
                Folder subFolder = folderRepository.save(createSubFolder("소분류", category, rootFolder));
                usersFolderRepository.save(createUsersFolder(user, subFolder, PermissionType.OWNER));

                // when
                boolean result = usersFolderRepository.existsFolderOwnerOrWriter(user.getId(), subFolder.getFolderId());

                // then
                assertThat(result).isTrue();
            }
        }

        @Nested
        @DisplayName("실패 케이스")
        class FailureCase {

            @Test
            @DisplayName("VIEWER 권한이면 false를 반환한다")
            void VIEWER_권한이면_false를_반환한다() {
                // given
                Category category = saveCategory("기술");
                Users owner = userRepository.save(createUser("owner3"));
                Users viewer = userRepository.save(createUser("viewer"));
                Folder folder = folderRepository.save(createFolder("공유 폴더2", category));
                usersFolderRepository.save(createUsersFolder(owner, folder, PermissionType.OWNER));
                usersFolderRepository.save(createUsersFolder(viewer, folder, PermissionType.VIEWER));

                // when
                boolean result = usersFolderRepository.existsFolderOwnerOrWriter(viewer.getId(), folder.getFolderId());

                // then
                assertThat(result).isFalse();
            }

            @Test
            @DisplayName("해당 폴더에 대한 권한 관계가 아예 없으면 false를 반환한다")
            void 권한_관계가_없으면_false를_반환한다() {
                // given
                Category category = saveCategory("기술");
                Users owner = userRepository.save(createUser("owner4"));
                Users stranger = userRepository.save(createUser("stranger"));
                Folder folder = folderRepository.save(createFolder("남의 폴더", category));
                usersFolderRepository.save(createUsersFolder(owner, folder, PermissionType.OWNER));

                // when
                boolean result = usersFolderRepository.existsFolderOwnerOrWriter(stranger.getId(), folder.getFolderId());

                // then
                assertThat(result).isFalse();
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

    private Folder createSubFolder(String name, Category category, Folder parentFolder) {
        return Folder.builder()
                .folderName(name)
                .category(category)
                .parentFolder(parentFolder)
                .build();
    }

    private UsersFolder createUsersFolder(Users user, Folder folder, PermissionType permissionType) {
        return UsersFolder.builder()
                .user(user)
                .folder(folder)
                .permissionType(permissionType)
                .isBookmarked(false)
                .build();
    }
}
