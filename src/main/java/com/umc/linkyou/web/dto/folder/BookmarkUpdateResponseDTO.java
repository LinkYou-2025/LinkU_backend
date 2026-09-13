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
public class BookmarkUpdateResponseDTO {
    private Long folderId;
    private Boolean isBookmarked;
}
