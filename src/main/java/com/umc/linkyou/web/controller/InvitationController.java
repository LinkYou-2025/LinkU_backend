package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.FolderSuccessStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.share.InvitationService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.folder.share.InvitationInfoResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.web.bind.annotation.*;

@Tag(name = "invitation-controller", description = "초대 관련 API")
@ApiV1
@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @Operation(summary = "초대장 미리보기", description = "토큰을 통해 초대된 폴더명과 초대자 닉네임을 확인합니다.")
    @GetMapping("/{token}")
    public ApiResponse<InvitationInfoResponseDTO> getInvitationInfo(
            @PathVariable String token
    ) {
        InvitationInfoResponseDTO info = invitationService.getInvitationInfo(token);
        return ApiResponse.onSuccess(FolderSuccessStatus.INVITATION_INFO_OK, info);
    }

    @Operation(summary = "초대 수락하기", description = "초대를 수락합니다.")
    @PostMapping("/{token}")
    public ApiResponse<Long> acceptInvitation(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable String token
    ) {
        Long folderId = invitationService.acceptInvitation(userDetails.getUserId(), token);
        return ApiResponse.onSuccess(FolderSuccessStatus.INVITATION_ACCEPTED, folderId);
    }
}
