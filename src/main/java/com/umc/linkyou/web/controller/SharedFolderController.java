package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.FolderSuccessStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.shared.SharedFolderService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.SharedFolderApi;
import com.umc.linkyou.web.dto.folder.share.SharedFolderGroupResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ApiV1
@RequiredArgsConstructor
public class SharedFolderController implements SharedFolderApi {

    private final SharedFolderService sharedFolderService;

    @Override
    public ApiResponse<List<SharedFolderGroupResponseDTO>> getSharedFolders(@CurrentUser CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_SHARED_OK, sharedFolderService.getSharedFolders(userDetails.getUserId()));
    }

    @Override
    public ApiResponse<Object> deleteSharedFolder(@CurrentUser CustomUserDetails userDetails, @PathVariable Long folderId) {
        sharedFolderService.deleteSharedFolder(userDetails.getUserId(), folderId);
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_LEAVE_OK);
    }
}
