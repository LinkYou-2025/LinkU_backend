package com.umc.linkyou.web.dto.folder.share;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MySharedFolderResponseDTO {
    private Long folderId;
    private String folderName;
    private int memberCount;
}
