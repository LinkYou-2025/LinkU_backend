package com.umc.linkyou.web.dto.folder.share;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FolderLeaveRequestDTO {
    @NotNull
    private Long newOwnerUserFolderId;
}
