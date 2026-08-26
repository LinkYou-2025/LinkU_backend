package com.umc.linkyou.converter;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.web.dto.folder.FolderResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderTreeResponseDTO;
import com.umc.linkyou.web.dto.folder.linku.FolderSummaryDTO;

import java.util.Map;

public class FolderConverter {

    public static FolderResponseDTO toFolderResponseDTO(Folder folder, Boolean isBookmarked) {
        if (folder == null) {
            return null;
        }
        Category category = folder.getCategory();
        return FolderResponseDTO.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .isBookmarked(isBookmarked)
                .categoryId(category != null ? category.getCategoryId() : null)
                .categoryName(category != null ? category.getCategoryName() : null)
                .parentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getFolderId() : null)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    public static FolderTreeResponseDTO toFolderTreeDTO(Folder folder, Map<Long, Boolean> bookmarkMap) {
        Category category = folder.getCategory();
        return FolderTreeResponseDTO.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .isBookmarked(bookmarkMap.getOrDefault(folder.getFolderId(), false))
                .categoryId(category != null ? category.getCategoryId() : null)
                .build();
    }

    public static FolderSummaryDTO toFolderSummaryDTO(Folder folder, boolean isBookmarked, boolean isSharing) {
        return FolderSummaryDTO.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .isBookmarked(isBookmarked)
                .isSharing(isSharing ? "share" : "private")
                .build();
    }

    public static Folder toFolder(Category category) {
        return Folder.builder()
                .category(category)
                .folderName(category.getCategoryName())
                .parentFolder(null)
                .build();
    }

    public static UsersFolder toUsersFolder(Users user, Folder folder, PermissionType permissionType) {
        return UsersFolder.builder()
                .user(user)
                .folder(folder)
                .permissionType(permissionType)
                .isBookmarked(false)
                .build();
    }
}
