package com.umc.linkyou.web.dto.folder;

import java.time.LocalDateTime;

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
public class FolderResponseDTO {
    private Long folderId;
    private String folderName;
    private Boolean isBookmarked;
    private Long categoryId;
    private String categoryName;
    private Long parentFolderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
