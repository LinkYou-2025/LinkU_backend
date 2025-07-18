package com.umc.linkyou.web.dto.folder;

import com.umc.linkyou.domain.common.BaseEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
public class FolderResponseDTO {
    private Long folderId;
    private String folderName;
    private Long categoryId;
    private String categoryName;
    private Long parentFolderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
