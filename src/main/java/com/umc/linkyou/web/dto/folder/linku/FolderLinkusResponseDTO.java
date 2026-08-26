package com.umc.linkyou.web.dto.folder.linku;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FolderLinkusResponseDTO {
    private List<FolderSummaryDTO> folders;
    private List<LinkuSummaryDTO> links;
    private String nextCursor;
}
