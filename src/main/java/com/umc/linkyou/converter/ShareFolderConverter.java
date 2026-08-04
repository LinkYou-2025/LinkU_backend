package com.umc.linkyou.converter;

import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.web.dto.folder.share.MySharedFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ViewerResponseDTO;

import java.time.LocalDateTime;

public class ShareFolderConverter {

    public static ShareFolderResponseDTO toShareFolderResponseDTO(
            Long folderId, Long userId, String permission, LocalDateTime sharedAt) {
        return ShareFolderResponseDTO.builder()
                .folderId(folderId)
                .userId(userId)
                .permission(permission)
                .sharedAt(sharedAt.toString())
                .build();
    }

    public static ViewerResponseDTO toViewerResponseDTO(UsersFolder usersFolder) {
        ViewerResponseDTO dto = new ViewerResponseDTO();
        dto.setUserId(usersFolder.getUser().getId());
        dto.setUserName(usersFolder.getUser().getNickName());
        dto.setPermission(usersFolder.getPermissionType().name());
        return dto;
    }

    public static MySharedFolderResponseDTO toMySharedFolderResponseDTO(Folder folder, int memberCount) {
        return MySharedFolderResponseDTO.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .memberCount(memberCount)
                .build();
    }
}
