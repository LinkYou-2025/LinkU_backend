package com.umc.linkyou.web.dto.folder.share;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.umc.linkyou.web.dto.folder.FolderTreeResponseDTO;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
