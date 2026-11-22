package com.umc.linkyou.web.dto.folder.share;

import java.util.List;

import com.umc.linkyou.web.dto.folder.FolderTreeResponseDTO;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SharedFolderTreeResponseDTO {
    private Long userId;
    private String nickname;
    private List<FolderTreeResponseDTO> folders;
}
