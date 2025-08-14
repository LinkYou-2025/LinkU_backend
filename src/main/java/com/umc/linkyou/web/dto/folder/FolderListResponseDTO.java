package com.umc.linkyou.web.dto.folder;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FolderListResponseDTO {
    private Long folderId;
    private String folderName;
    private Long parentFolderId;
    private Boolean isBookmarked;
    private String isSharing;
}

