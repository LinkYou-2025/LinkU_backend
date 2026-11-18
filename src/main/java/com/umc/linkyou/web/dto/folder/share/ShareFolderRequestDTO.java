package com.umc.linkyou.web.dto.folder.share;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShareFolderRequestDTO {
    private Long userId;
    private String permission;
}
