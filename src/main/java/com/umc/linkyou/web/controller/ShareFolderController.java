package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.share.ShareFolderService;
import com.umc.linkyou.web.dto.folder.share.FolderPermissionRequestDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderRequestDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ViewerResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "share-folder-controller", description = "폴더 공유 관련 API")
@RestController
@RequestMapping("/api/folders/share")
@RequiredArgsConstructor
public class ShareFolderController {
    private final ShareFolderService shareFolderService;

    @Operation(
            summary = "폴더 공유 (뷰어 권한 설정)",
            description = "폴더를 다른 사용자와 공유하고 뷰어 권한을 부여합니다."
    )
    @PostMapping("/{folderId}")
    public ApiResponse<ShareFolderResponseDTO> shareFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        ShareFolderRequestDTO request = new ShareFolderRequestDTO();
        request.setUserId(userDetails.getUsers().getId());
        request.setPermission("VIEWER");
        ShareFolderResponseDTO response = shareFolderService.shareFolder(
                userDetails.getUsers().getId(), folderId, request);
        return ApiResponse.of(SuccessStatus._FOLDER_SHARE_OK, response);
    }

    @Operation(
            summary = "폴더 뷰어 조회",
            description = "공유된 폴더의 뷰어 목록을 조회합니다."
    )
    @GetMapping("/{folderId}/members")
    public ApiResponse<List<ViewerResponseDTO>> getFolderViewers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        List<ViewerResponseDTO> viewers = shareFolderService.getViewers(
                userDetails.getUsers().getId(), folderId);
        return ApiResponse.of(SuccessStatus._FOLDER_MEMBERS_OK, viewers);
    }

    @Operation(
            summary = "폴더 권한 수정",
            description = "폴더 공유 멤버의 권한(뷰어/라이터)을 수정합니다."
    )
    @PutMapping("/{folderId}/members/{userFolderId}")
    public ApiResponse<ShareFolderResponseDTO> updateViewerPermission(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @PathVariable Long userFolderId,
            @Valid @RequestBody FolderPermissionRequestDTO request
    ) {
        ShareFolderResponseDTO response = shareFolderService.updateViewerPermission(
                userDetails.getUsers().getId(), folderId, userFolderId, request);
        return ApiResponse.of(SuccessStatus._FOLDER_PERMISSION_OK, response);
    }

    @Operation(
            summary = "폴더 비공개 전환",
            description = "공유된 폴더를 비공개로 전환하고 모든 공유 권한을 제거합니다."
    )
    @PostMapping("/{folderId}/unshare")
    public ApiResponse<ShareFolderResponseDTO> unshareFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        // 모든 유저의 (폴더 주인 제외) 뷰어, writer 권한 false
        ShareFolderResponseDTO response = shareFolderService.unshare(userDetails.getUsers().getId(), folderId);
        return ApiResponse.of(SuccessStatus._FOLDER_UNSHARE_OK, response);
    }
}