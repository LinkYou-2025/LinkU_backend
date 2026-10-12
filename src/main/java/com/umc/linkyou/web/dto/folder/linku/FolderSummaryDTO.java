package com.umc.linkyou.web.dto.folder.linku;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// 중폴더 내부에 있는 소분류 폴더 응답 형식
public class FolderSummaryDTO {
    private Long folderId;
    private String folderName;
    private Boolean isBookmarked;
    private String isSharing;
}
