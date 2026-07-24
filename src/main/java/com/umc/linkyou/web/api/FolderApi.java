package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.folder.*;
import com.umc.linkyou.web.dto.folder.linku.FolderLinkusResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "폴더 API", description = "폴더와 관련 된 API 입니다")
@RequestMapping("/folders")
public interface FolderApi {

    @Operation(summary = "소분류 폴더 생성", description = "중분류 폴더 하위에 소분류 폴더를 생성합니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_PARENT_NOT_FOUND, FolderErrorStatus._FOLDER_MAX_DEPTH_EXCEEDED, FolderErrorStatus._FOLDER_CREATE_FORBIDDEN, FolderErrorStatus._FOLDER_NAME_CONFLICT, FolderErrorStatus._FOLDER_CREATE_DUPLICATE})
    @PostMapping("/{parentFolderId}/subfolders")
    ApiResponse<FolderResponseDTO> createFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long parentFolderId,
            @RequestBody FolderCreateRequestDTO request
    );

    @Operation(summary = "소분류 폴더 수정", description = "기존 소분류 폴더의 정보를 수정합니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND, FolderErrorStatus._FOLDER_UPDATE_FORBIDDEN, FolderErrorStatus._FOLDER_PARENT_NOT_FOUND, FolderErrorStatus._FOLDER_NAME_CONFLICT, FolderErrorStatus._FOLDER_CREATE_DUPLICATE})
    @PutMapping("/subfolders/{folderId}")
    ApiResponse<FolderResponseDTO> updateFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @RequestBody FolderUpdateRequestDTO request
    );

    @Operation(summary = "소분류 폴더 삭제", description = "소분류 폴더를 삭제합니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND, FolderErrorStatus._FOLDER_DELETE_FORBIDDEN})
    @DeleteMapping("/subfolders/{folderId}")
    ApiResponse<Object> deleteFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    );

    @Operation(summary = "내 폴더 목록(트리) 조회", description = "사용자의 모든 폴더를 트리 구조로 조회합니다.")
    @GetMapping("/my")
    ApiResponse<List<FolderTreeResponseDTO>> getMyFolderTree(
            @CurrentUser CustomUserDetails userDetails
    );

    @Operation(summary = "중분류 폴더 조회", description = "사용자의 모든 중분류 폴더 목록을 조회합니다. sort: name(가나다순, 기본값), updatedAt(최근 수정순)")
    @GetMapping("/parentFolders")
    ApiResponse<List<FolderListResponseDTO>> getParentFolderList(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(defaultValue = "name") String sort
    );

    @Operation(summary = "중분류 내부의 하위 폴더 조회", description = "특정 중분류 폴더의 하위 소분류 폴더 목록을 조회합니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND, FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN})
    @GetMapping("/{parentFolderId}/subfolders")
    ApiResponse<List<FolderListResponseDTO>> getSubFolderList(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long parentFolderId
    );

    @Operation(summary = "북마크 설정/해제", description = "폴더의 북마크 상태를 설정하거나 해제합니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND}, errorStatus = {ErrorStatus._FOLDER_BOOKMARK_NOT_FOUND})
    @PatchMapping("/{folderId}/bookmark")
    ApiResponse<BookmarkUpdateResponseDTO> updateBookmark(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @RequestBody BookmarkUpdateRequestDTO request
    );

    @Operation(summary = "폴더 내부 링크, 폴더 목록 조회", description = "특정 폴더 내부의 링크와 하위 폴더 목록을 조회합니다. 커서 기반 페이지네이션을 지원합니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND, FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN, FolderErrorStatus._FOLDER_INVALID_CURSOR})
    @GetMapping("/{folderId}/linkus")
    ApiResponse<FolderLinkusResponseDTO> getFolderLinkus(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "20") @Min(1) int limit,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "name") String sort
    );
}
