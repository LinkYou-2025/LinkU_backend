package com.umc.linkyou.web.dto.folder.linku;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderLinkusResponseDTO {
    private List<FolderSummaryDTO> folders;
    private List<LinkuSummaryDTO> links;
    private String nextCursor;
}
