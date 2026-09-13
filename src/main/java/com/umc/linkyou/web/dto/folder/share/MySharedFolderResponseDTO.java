package com.umc.linkyou.web.dto.folder.share;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MySharedFolderResponseDTO {
    private Long folderId;
    private String folderName;
    private int memberCount;
}
