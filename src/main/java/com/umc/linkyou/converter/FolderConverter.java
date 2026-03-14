package com.umc.linkyou.converter;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.web.dto.folder.FolderResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderTreeResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class FolderConverter {

    public FolderResponseDTO toFolderResponseDTO(Folder folder) {
        if (folder == null) {
            return null;
        }
        Category category = folder.getCategory();
        return FolderResponseDTO.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .categoryId(category != null ? category.getCategoryId() : null)
                .categoryName(category != null ? category.getCategoryName() : null)
                .parentFolderId(folder.getParentFolder() != null ? folder.getParentFolder().getFolderId() : null)
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    public FolderResponseDTO toFolderResponseDTO(Folder folder, Boolean isBookmarked) {
        FolderResponseDTO dto = toFolderResponseDTO(folder);
        if (dto != null) {
            dto.setIsBookmarked(isBookmarked);
        }
        return dto;
    }

    public FolderTreeResponseDTO toFolderTreeDTO(Folder folder, Map<Long, Boolean> bookmarkMap) {
        FolderTreeResponseDTO dto = new FolderTreeResponseDTO();

        dto.setFolderId(folder.getFolderId());
        dto.setFolderName(folder.getFolderName());
        dto.setIsBookmarked(bookmarkMap.getOrDefault(folder.getFolderId(), false));

        Category category = folder.getCategory();
        if (category != null) {
            dto.setCategoryId(category.getCategoryId());
        }

        return dto;
    }

    public Folder toFolder(Category category) {
        return Folder.builder()
                .category(category)
                .folderName(category.getCategoryName())
                .parentFolder(null)
                .build();
    }

    public UsersFolder toUsersFolder(Users user, Folder folder) {
        return UsersFolder.builder()
                .user(user)
                .folder(folder)
                .permissionType(PermissionType.OWNER)
                .isBookmarked(false)
                .build();
    }
}
