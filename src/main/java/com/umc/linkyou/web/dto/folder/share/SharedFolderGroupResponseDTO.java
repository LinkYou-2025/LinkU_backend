package com.umc.linkyou.web.dto.folder.share;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SharedFolderGroupResponseDTO {
    private Long userId;
    private String nickname;
    private List<SharedFolderItemDTO> folders;
}
