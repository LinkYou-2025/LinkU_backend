package com.umc.linkyou.service.folder.share;

import com.umc.linkyou.web.dto.folder.share.InvitationInfoResponseDTO;

public interface InvitationService {
    InvitationInfoResponseDTO getInvitationInfo(String token);
    Long acceptInvitation(Long userId, String token);
}
