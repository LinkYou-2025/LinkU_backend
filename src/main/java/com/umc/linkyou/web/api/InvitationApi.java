package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.InvitationErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.folder.share.InvitationInfoResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "초대장 API", description = "공유 폴더에 참가 가능한 초대장과 관련 된 API 입니다")
@RequestMapping("/invitations")
public interface InvitationApi {

    @Operation(summary = "초대장 미리보기", description = "토큰을 통해 초대된 폴더명과 초대자 닉네임을 확인합니다.")
    @ApiErrorCode(invitationErrorStatus = {InvitationErrorStatus.INVITATION_NOT_FOUND, InvitationErrorStatus.INVITATION_EXPIRED})
    @GetMapping("/{token}")
    ApiResponse<InvitationInfoResponseDTO> getInvitationInfo(
            @PathVariable String token
    );

    @Operation(summary = "초대 수락하기", description = "초대를 수락합니다.")
    @ApiErrorCode(invitationErrorStatus = {InvitationErrorStatus.INVITATION_NOT_FOUND, InvitationErrorStatus.INVITATION_EXPIRED, InvitationErrorStatus.INVITATION_CREATOR_CANNOT_ACCEPT})
    @PostMapping("/{token}")
    ApiResponse<Long> acceptInvitation(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable String token
    );
}
