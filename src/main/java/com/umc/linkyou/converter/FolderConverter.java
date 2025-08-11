package com.umc.linkyou.converter;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.folder.FolderResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderTreeResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FolderConverter {
    private final UsersFolderRepository usersFolderRepository;

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

    public FolderTreeResponseDTO toFolderTreeDTO(Folder folder, Long userId) {
        FolderTreeResponseDTO dto = new FolderTreeResponseDTO();

        dto.setFolderId(folder.getFolderId());
        dto.setFolderName(folder.getFolderName());

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
                .parentFolder(null)  // 필요 시 상위 폴더 지정
                .build();
    }
    public UsersFolder toUsersFolder(Users user, Folder folder) {
        return UsersFolder.builder()
                .user(user)
                .folder(folder)
                .isOwner(true)
                .isViewer(true)
                .isWriter(true)
                .isBookmarked(false)
                .build();
    }
}

