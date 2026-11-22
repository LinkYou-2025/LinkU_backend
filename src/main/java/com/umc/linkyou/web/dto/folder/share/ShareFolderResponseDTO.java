package com.umc.linkyou.web.dto.folder.share;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShareFolderResponseDTO {
    private Long folderId;
    private Long userId;
    private String permission;
    private String sharedAt;

    @Builder
    public ShareFolderResponseDTO(Long folderId, Long userId, String permission, String sharedAt) {
        this.folderId = folderId;
        this.userId = userId;
        this.permission = permission;
        this.sharedAt = sharedAt;
    }
}
