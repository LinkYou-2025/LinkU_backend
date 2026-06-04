package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.FolderSuccessStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.share.ShareFolderService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.ShareFolderApi;
import com.umc.linkyou.web.dto.folder.share.FolderPermissionRequestDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ViewerResponseDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ApiV1
@RequiredArgsConstructor
public class ShareFolderController implements ShareFolderApi {

    private final ShareFolderService shareFolderService;

    @Value("${app.deeplink.base-url}")
    private String deeplinkBaseUrl;

    @Override
    public ApiResponse<String> createInviteLink(@CurrentUser CustomUserDetails userDetails, @PathVariable Long folderId) {
        String token = shareFolderService.createInviteLink(userDetails.getUserId(), folderId);
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_INVITE_LINK_CREATED, deeplinkBaseUrl + "/open?action=share&folderId=" + token);
    }

    @Override
    public ApiResponse<Object> deactivateInviteLink(@CurrentUser CustomUserDetails userDetails, @PathVariable Long folderId) {
        shareFolderService.deactivateInviteLink(userDetails.getUserId(), folderId);
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_INVITE_LINK_DEACTIVATED);
    }

    @Override
    public ApiResponse<List<ViewerResponseDTO>> getFolderViewers(@CurrentUser CustomUserDetails userDetails, @PathVariable Long folderId) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_MEMBERS_OK, shareFolderService.getViewers(userDetails.getUserId(), folderId));
    }

    @Override
    public ApiResponse<ShareFolderResponseDTO> updateViewerPermission(@CurrentUser CustomUserDetails userDetails, @PathVariable Long folderId, @PathVariable Long userFolderId, @Valid @RequestBody FolderPermissionRequestDTO request) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_PERMISSION_OK, shareFolderService.updateViewerPermission(userDetails.getUserId(), folderId, userFolderId, request));
    }

    @Override
    public ApiResponse<ShareFolderResponseDTO> unshareFolder(@CurrentUser CustomUserDetails userDetails, @PathVariable Long folderId) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_UNSHARE_OK, shareFolderService.unshare(userDetails.getUserId(), folderId));
    }
}
