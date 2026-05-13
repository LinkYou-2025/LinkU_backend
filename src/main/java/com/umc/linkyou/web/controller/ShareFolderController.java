package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.FolderSuccessStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.share.ShareFolderService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.folder.share.FolderPermissionRequestDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ViewerResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "share-folder-controller", description = "폴더 공유 관련 API")
@ApiV1
@RestController
@RequestMapping("/folders/share")
@RequiredArgsConstructor
public class ShareFolderController {
    private final ShareFolderService shareFolderService;

    @Value("${app.deeplink.base-url}")
    private String deeplinkBaseUrl;

    @Operation(summary = "초대 링크 생성", description = "해당 폴더의 초대용 토큰을 생성합니다.")
    @PostMapping("/{folderId}/invitation")
    public ApiResponse<String> createInviteLink(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        String token = shareFolderService.createInviteLink(userDetails.getUserId(), folderId);
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_INVITE_LINK_CREATED, deeplinkBaseUrl + "/open?action=share&folderId=" + token);
    }

    @Operation(summary = "초대 링크 삭제", description = "초대 링크를 비활성화합니다.")
    @DeleteMapping("/{folderId}/invitation")
    public ApiResponse<Void> deactivateInviteLink(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        shareFolderService.deactivateInviteLink(userDetails.getUserId(), folderId);
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_INVITE_LINK_DEACTIVATED, null);
    }

    @Operation(summary = "폴더 멤버 조회", description = "공유된 폴더의 멤버 목록을 조회합니다.")
    @GetMapping("/{folderId}/members")
    public ApiResponse<List<ViewerResponseDTO>> getFolderViewers(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        List<ViewerResponseDTO> viewers = shareFolderService.getViewers(userDetails.getUserId(), folderId);
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_MEMBERS_OK, viewers);
    }

    @Operation(summary = "폴더 권한 수정", description = "폴더 공유 멤버의 권한(뷰어/라이터)을 수정합니다.")
    @PutMapping("/{folderId}/members/{userFolderId}")
    public ApiResponse<ShareFolderResponseDTO> updateViewerPermission(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @PathVariable Long userFolderId,
            @Valid @RequestBody FolderPermissionRequestDTO request
    ) {
        ShareFolderResponseDTO response = shareFolderService.updateViewerPermission(
                userDetails.getUserId(), folderId, userFolderId, request);
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_PERMISSION_OK, response);
    }

    @Operation(summary = "폴더 비공개 전환", description = "공유된 폴더를 비공개로 전환하고 모든 공유 권한을 제거합니다.")
    @PostMapping("/{folderId}/unshare")
    public ApiResponse<ShareFolderResponseDTO> unshareFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        ShareFolderResponseDTO response = shareFolderService.unshare(userDetails.getUserId(), folderId);
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_UNSHARE_OK, response);
    }
}
