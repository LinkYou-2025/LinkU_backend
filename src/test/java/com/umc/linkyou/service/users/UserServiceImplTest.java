package com.umc.linkyou.service.users;

import com.umc.linkyou.domain.RecentViewedLinku;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.RecentViewedLinkuRepository;
import com.umc.linkyou.repository.categoryRepository.UsersCategoryColorRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@AutoConfigureRestDocs
@TestPropertySource(properties = "spring.jpa.show-sql=true")
class UserServiceImplTest {

    @Autowired private UserServiceImpl userService;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    @Autowired private RecentViewedLinkuRepository recentViewedLinkuRepository;
    @Autowired private UsersFolderRepository usersFolderRepository;
    @Autowired private UsersCategoryColorRepository usersCategoryColorRepository;

    @Test
    @DisplayName("✅ CASCADE 테스트: Users + RecentViewedLinku + UsersFolder + UsersCategoryColor")
    void testCascadeDelete() {
        // ===== 1. 10일 지난 INACTIVE 사용자 생성 =====
        Users targetUser = createInactiveUser("cascade@test.com", LocalDateTime.now().minusDays(11));

        // ===== 2. CASCADE 대상 데이터 생성 =====
        // RecentViewedLinku (linku_id는 null 허용으로 테스트)
        RecentViewedLinku linku = RecentViewedLinku.builder()
                .user(targetUser)
                .linku(null)  // Linku는 필수 아니므로 null
                .viewedAt(LocalDateTime.now())
                .build();
        recentViewedLinkuRepository.save(linku);

        // UsersFolder
        Folder testFolder = Folder.builder()
                .folderName("test-folder")
                .build();
        UsersFolder folder = UsersFolder.builder()
                .user(targetUser)
                .folder(testFolder)
                .isOwner(true)
                .isViewer(true)
                .isWriter(true)
                .build();
        usersFolderRepository.save(folder);

        // UsersCategoryColor
        Category testCategory = Category.builder()
                .categoryName("test-category")
                .build();
        Fcolor testColor = Fcolor.builder()
                .colorName("test-color")
                .build();
        UsersCategoryColor color = UsersCategoryColor.builder()
                .user(targetUser)
                .category(testCategory)
                .fcolor(testColor)
                .build();
        usersCategoryColorRepository.save(color);

        // ===== 3. Flush & Clear =====
        em.flush();
        em.clear();

        // ===== 4. 삭제 실행 =====
        userService.deleteCompletelyInactiveUsers();

        // ===== 5. CASCADE 확인 =====
        assertFalse(userRepository.existsById(targetUser.getId()));
        assertFalse(recentViewedLinkuRepository.existsById(linku.getRecentViewedLinkuId()));
        assertFalse(usersFolderRepository.existsById(folder.getUserFolderId()));
        assertFalse(usersCategoryColorRepository.existsById(color.getUserCategoryColorId()));

        System.out.println("🎉 CASCADE 테스트 성공!");
        System.out.println("✅ Users, RecentViewedLinku, UsersFolder, UsersCategoryColor 모두 삭제됨");
    }

    @Test
    @DisplayName("🔄 최근 사용자 (5일 이내)는 삭제 안됨")
    void testRecentUserNotDeleted() {
        Users recentUser = createInactiveUser("recent@test.com", LocalDateTime.now().minusDays(5));

        em.flush();
        em.clear();

        userService.deleteCompletelyInactiveUsers();

        assertTrue(userRepository.existsById(recentUser.getId()));
        System.out.println("✅ 최근 사용자 보존 성공!");
    }

    private Users createInactiveUser(String email, LocalDateTime inactiveDate) {
        Users user = Users.builder()
                .email(email)
                .nickName(email.split("@")[0])
                .status("INACTIVE")
                .inactiveDate(inactiveDate)
                .build();
        return userRepository.save(user);
    }
}
