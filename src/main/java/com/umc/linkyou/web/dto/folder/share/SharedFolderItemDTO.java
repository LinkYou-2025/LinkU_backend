package com.umc.linkyou.web.dto.folder.share;

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
public class SharedFolderItemDTO {
    private Long folderId;
    private String folderName;
    private Boolean isBookmarked;
    private Long categoryId;
}
