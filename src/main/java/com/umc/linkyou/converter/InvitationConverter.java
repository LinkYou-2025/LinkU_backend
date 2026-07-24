package com.umc.linkyou.converter;

import com.umc.linkyou.domain.folder.FolderShareLink;
import com.umc.linkyou.web.dto.folder.share.InvitationInfoResponseDTO;

public class InvitationConverter {

    public static InvitationInfoResponseDTO toInvitationInfoResponseDTO(FolderShareLink link) {
        return InvitationInfoResponseDTO.builder()
                .folderName(link.getFolder().getFolderName())
                .ownerName(link.getCreator().getNickName())
                .build();
    }
}
