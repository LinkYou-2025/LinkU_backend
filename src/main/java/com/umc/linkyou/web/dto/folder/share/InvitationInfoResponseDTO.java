package com.umc.linkyou.web.dto.folder.share;

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
public class InvitationInfoResponseDTO {
    private String folderName;
    private String ownerName;
}