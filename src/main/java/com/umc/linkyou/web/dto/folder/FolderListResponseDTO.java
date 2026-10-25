package com.umc.linkyou.web.dto.folder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderListResponseDTO {
    private Long folderId;
    private String folderName;
    private Long parentFolderId;
    private Boolean isBookmarked;
    private String isSharing;
    private Long categoryId;
}