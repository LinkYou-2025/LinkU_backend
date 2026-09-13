package com.umc.linkyou.support.fixture;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;

public class FolderFixture {
    public static final Long OWNER_ID = 1L;
    public static final Long FOLDER_ID = 100L;
    public static final Long PARENT_FOLDER_ID = 200L;
    public static final Long CATEGORY_ID = 10L;

    public static Users owner() {
        return
                Users.builder()
                        .id(OWNER_ID)
                        .nickName("주인")
                        .role(Role.USER)
                        .build();
    }

    public static Category category() {
        return Category.builder().categoryId(CATEGORY_ID).categoryName("카테고리").build();
    }

    public static Folder folder() {
        return
                Folder.builder()
                        .folderId(FOLDER_ID)
                        .folderName("어학").build();
    }

    public static Folder parentFolder() {
        return Folder.builder()
                .folderId(PARENT_FOLDER_ID)
                .folderName("중분류")
                .category(category())
                .build();
    }

    public static Folder subFolder(Folder parent) {
        return Folder.builder()
                .folderId(FOLDER_ID)
                .folderName("어학")
                .category(parent.getCategory())
                .parentFolder(parent)
                .build();
    }

    public static UsersFolder participant(Long userId, PermissionType type) {
        Users user = Users.builder().id(userId).nickName("user" + userId).role(Role.USER).build();
        return UsersFolder.builder().user(user).folder(folder()).permissionType(type).build();
    }
}
