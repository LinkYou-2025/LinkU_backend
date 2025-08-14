package com.umc.linkyou.repository.usersFolderRepository;

import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsersFolderRepositoryCustom {
    List<Folder> searchFolders(
            Long userId,
            Long categoryId,
            Long parentFolderId,
            String folderName,
            Boolean isBookmarked,
            Boolean isShared
    );

    Optional<UsersFolder> findByUserIdAndFolderId(Long userId, Long folderId);

    List<UsersFolder> findParentFolders(Long userId);

    Optional<Folder> findFolderByUserIdAndFolderName(Long userId, String folderName);
}
