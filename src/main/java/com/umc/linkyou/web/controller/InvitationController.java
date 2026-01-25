package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.share.InvitationService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.web.dto.folder.share.InvitationInfoResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "invitation-controller", description = "초대 관련 API")
@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;
    private final UsersUtils usersUtils;

    @Operation(summary = "초대장 미리보기", description = "토큰을 통해 초대된 폴더명과 초대자 닉네임을 확인합니다.")
    @GetMapping("/{token}")
    public ApiResponse<InvitationInfoResponseDTO> getInvitationInfo(
            @PathVariable String token
    ) {
        InvitationInfoResponseDTO info = invitationService.getInvitationInfo(token);
        return ApiResponse.of(SuccessStatus._OK, info);
    }

    @Operation(summary = "초대 수락하기", description = "초대를 수락합니다.")
    @PostMapping("/{token}")
    public ApiResponse<Long> acceptInvitation(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String token
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        Long folderId = invitationService.acceptInvitation(userId, token);
        return ApiResponse.of(SuccessStatus._OK, folderId);
    }
}