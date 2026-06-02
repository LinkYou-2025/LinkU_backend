package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.FolderSuccessStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.share.InvitationService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.InvitationApi;
import com.umc.linkyou.web.dto.folder.share.InvitationInfoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@ApiV1
@RequiredArgsConstructor
public class InvitationController implements InvitationApi {

    private final InvitationService invitationService;

    @Override
    public ApiResponse<InvitationInfoResponseDTO> getInvitationInfo(@PathVariable String token) {
        return ApiResponse.onSuccess(FolderSuccessStatus.INVITATION_INFO_OK, invitationService.getInvitationInfo(token));
    }

    @Override
    public ApiResponse<Long> acceptInvitation(@CurrentUser CustomUserDetails userDetails, @PathVariable String token) {
        return ApiResponse.onSuccess(FolderSuccessStatus.INVITATION_ACCEPTED, invitationService.acceptInvitation(userDetails.getUserId(), token));
    }
}
