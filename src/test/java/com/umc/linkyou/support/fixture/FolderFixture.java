package com.umc.linkyou.support.fixture;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;

public class FolderFixture {
    public static final Long OWNER_ID = 1L;
    public static final Long FOLDER_ID = 100L;

    public static Users owner() {
        return
                Users.builder()
                        .id(OWNER_ID)
                        .nickName("주인")
                        .role(Role.USER)
                        .build();
    }

    public static Folder folder() {
        return
                Folder.builder()
                        .folderId(FOLDER_ID)
                        .folderName("어학").build();
    }

    public static UsersFolder participant(Long userId, PermissionType type) {
        Users user = Users.builder().id(userId).nickName("user" + userId).role(Role.USER).build();
        return UsersFolder.builder().user(user).folder(folder()).permissionType(type).build();
    }
}
