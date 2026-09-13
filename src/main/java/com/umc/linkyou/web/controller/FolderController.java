package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.FolderSuccessStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.FolderService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.FolderApi;
import com.umc.linkyou.web.dto.folder.*;
import com.umc.linkyou.web.dto.folder.linku.FolderLinkusResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ApiV1
@RequiredArgsConstructor
@Validated
public class FolderController implements FolderApi {

    private final FolderService folderService;

    @Override
    public ApiResponse<FolderResponseDTO> createFolder(@CurrentUser CustomUserDetails userDetails, @PathVariable Long parentFolderId, @RequestBody FolderCreateRequestDTO request) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_CREATED, folderService.createFolder(userDetails.getUserId(), parentFolderId, request));
    }

    @Override
    public ApiResponse<FolderResponseDTO> updateFolder(@CurrentUser CustomUserDetails userDetails, @PathVariable Long folderId, @RequestBody FolderUpdateRequestDTO request) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_UPDATED, folderService.updateFolder(userDetails.getUserId(), folderId, request));
    }

    @Override
    public ApiResponse<Object> deleteFolder(@CurrentUser CustomUserDetails userDetails, @PathVariable Long folderId) {
        folderService.deleteFolder(userDetails.getUserId(), folderId);
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_DELETED);
    }

    @Override
    public ApiResponse<List<FolderTreeResponseDTO>> getMyFolderTree(@CurrentUser CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_OK, folderService.getMyFolderTree(userDetails.getUserId()));
    }

    @Override
    public ApiResponse<List<FolderListResponseDTO>> getParentFolderList(@CurrentUser CustomUserDetails userDetails, @RequestParam(defaultValue = "name") String sort) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_PARENT_OK, folderService.getParentFolders(userDetails.getUserId(), sort));
    }

    @Override
    public ApiResponse<List<FolderListResponseDTO>> getSubFolderList(@CurrentUser CustomUserDetails userDetails, @PathVariable Long parentFolderId) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_SUBFOLDER_OK, folderService.getSubFolders(userDetails.getUserId(), parentFolderId));
    }

    @Override
    public ApiResponse<BookmarkUpdateResponseDTO> updateBookmark(@CurrentUser CustomUserDetails userDetails, @PathVariable Long folderId, @RequestBody BookmarkUpdateRequestDTO request) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_BOOKMARK_OK, folderService.updateBookmark(userDetails.getUserId(), folderId, request.getIsBookmarked()));
    }

    @Override
    public ApiResponse<FolderLinkusResponseDTO> getFolderLinkus(@CurrentUser CustomUserDetails userDetails, @PathVariable Long folderId, @RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "name") String sort, @RequestParam(defaultValue = "true") boolean includeLinks) {
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_LINK_OK, folderService.getFolderLinkus(userDetails.getUserId(), folderId, limit, cursor, sort, includeLinks));
    }
}
