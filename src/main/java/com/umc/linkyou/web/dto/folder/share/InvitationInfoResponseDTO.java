package com.umc.linkyou.web.dto.folder.share;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class InvitationInfoResponseDTO {
    private String folderName;
    private String ownerName;
}