package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.FolderSuccessStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.FolderService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.folder.*;
import com.umc.linkyou.web.dto.folder.linku.FolderLinkusResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "folder-controller", description = "폴더 관련 API")
@ApiV1
@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
@Validated
public class FolderController {
    private final FolderService folderService;

    @Operation(
            summary = "소분류 폴더 생성",
            description = "중분류 폴더 하위에 소분류 폴더를 생성합니다."
    )
    @PostMapping("/{parentFolderId}/subfolders")
    public ApiResponse<FolderResponseDTO> createFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long parentFolderId,
            @RequestBody FolderCreateRequestDTO request
    ) {
        FolderResponseDTO response = folderService.createFolder(userDetails.getUserId(), parentFolderId, request);
        return ApiResponse.of(FolderSuccessStatus.FOLDER_CREATED, response);
    }

    @Operation(
            summary = "소분류 폴더 수정",
            description = "기존 소분류 폴더의 정보를 수정합니다."
    )
    @PutMapping("/subfolders/{folderId}")
    public ApiResponse<FolderResponseDTO> updateFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @RequestBody FolderUpdateRequestDTO request
    ) {
        FolderResponseDTO response = folderService.updateFolder(userDetails.getUserId(), folderId, request);
        return ApiResponse.of(FolderSuccessStatus.FOLDER_UPDATED, response);
    }

    @Operation(
            summary = "소분류 폴더 삭제",
            description = "소분류 폴더를 삭제합니다."
    )
    @DeleteMapping("/subfolders/{folderId}")
    public ApiResponse<Void> deleteFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        folderService.deleteFolder(userDetails.getUserId(), folderId);
        return ApiResponse.of(FolderSuccessStatus.FOLDER_DELETED, null);
    }

    @Operation(
            summary = "내 폴더 목록(트리) 조회",
            description = "사용자의 모든 폴더를 트리 구조로 조회합니다."
    )
    @GetMapping("/my")
    public ApiResponse<List<FolderTreeResponseDTO>> getMyFolderTree(
            @CurrentUser CustomUserDetails userDetails
    ) {
        List<FolderTreeResponseDTO> folderTree = folderService.getMyFolderTree(userDetails.getUserId());
        return ApiResponse.of(FolderSuccessStatus.FOLDER_OK, folderTree);
    }

    @Operation(
            summary = "중분류 폴더 조회",
            description = "사용자의 모든 중분류 폴더 목록을 조회합니다. sort: name(가나다순, 기본값), updatedAt(최근 수정순)"
    )
    @GetMapping("/parentFolders")
    public ApiResponse<List<FolderListResponseDTO>> getParentFolderList(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(defaultValue = "name") String sort
    ) {
        List<FolderListResponseDTO> folderList = folderService.getParentFolders(userDetails.getUserId(), sort);
        return ApiResponse.of(FolderSuccessStatus.FOLDER_PARENT_OK, folderList);
    }

    @Operation(
            summary = "중분류 내부의 하위 폴더 조회",
            description = "특정 중분류 폴더의 하위 소분류 폴더 목록을 조회합니다."
    )
    @GetMapping("/{parentFolderId}/subfolders")
    public ApiResponse<List<FolderListResponseDTO>> getSubFolderList(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long parentFolderId
    ) {
        List<FolderListResponseDTO> folderList = folderService.getSubFolders(userDetails.getUserId(), parentFolderId);
        return ApiResponse.of(FolderSuccessStatus.FOLDER_SUBFOLDER_OK, folderList);
    }

    @Operation(
            summary = "북마크 설정/해제",
            description = "폴더의 북마크 상태를 설정하거나 해제합니다."
    )
    @PatchMapping("/{folderId}/bookmark")
    public ApiResponse<BookmarkUpdateResponseDTO> updateBookmark(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @RequestBody BookmarkUpdateRequestDTO request
    ) {
        BookmarkUpdateResponseDTO response = folderService.updateBookmark(
                userDetails.getUserId(), folderId, request.getIsBookmarked()
        );
        return ApiResponse.of(FolderSuccessStatus.FOLDER_BOOKMARK_OK, response);
    }

    @Operation(
            summary = "폴더 내부 링크, 폴더 목록 조회",
            description = "특정 폴더 내부의 링크와 하위 폴더 목록을 조회합니다. 커서 기반 페이지네이션을 지원합니다."
    )
    @GetMapping("/{folderId}/linkus")
    public ApiResponse<FolderLinkusResponseDTO> getFolderLinkus(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "name") String sort
    ) {
        FolderLinkusResponseDTO response = folderService.getFolderLinkus(
                userDetails.getUserId(), folderId, limit, cursor, sort);
        return ApiResponse.of(FolderSuccessStatus.FOLDER_LINK_OK, response);
    }
}
