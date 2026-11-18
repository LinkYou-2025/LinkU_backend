package com.umc.linkyou.web.dto.folder.share;

import com.umc.linkyou.domain.enums.PermissionType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FolderPermissionRequestDTO {
    private PermissionType permission;
}
