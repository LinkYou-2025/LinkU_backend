package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.FolderService;
import com.umc.linkyou.web.dto.folder.*;
import com.umc.linkyou.web.dto.folder.linku.FolderLinkusResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {
    private final FolderService folderService;

    // (소분류) 폴더 생성
    @PostMapping("/{parentFolderId}/subfolders")
    @Operation(summary = "소분류 폴더 생성")
    public ApiResponse<FolderResponseDTO> createFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long parentFolderId,
            @RequestBody FolderCreateRequestDTO request
    ) {
        FolderResponseDTO response = folderService.createFolder(userDetails.getUsers().getId(), parentFolderId, request);
        return ApiResponse.of(SuccessStatus._FOLDER_CREATE_OK, response);
    }

    // (소분류) 폴더 수정
    @PutMapping("/subfolders/{folderId}")
    @Operation(summary = "소분류 폴더 수정")
    public ApiResponse<FolderResponseDTO> updateFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @RequestBody FolderUpdateRequestDTO request
    ) {
        FolderResponseDTO response = folderService.updateFolder(userDetails.getUsers().getId(), folderId, request);
        return ApiResponse.of(SuccessStatus._FOLDER_UPDATE_OK, response);
    }

   // (소분류) 폴더 삭제
    @DeleteMapping("/subfolders/{folderId}")
    @Operation(summary = "소분류 폴더 삭제")
    public ResponseEntity<FolderResponseDTO> deleteFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        FolderResponseDTO response = folderService.deleteFolder(userDetails.getUsers().getId(), folderId);
        return ResponseEntity.ok(response);
    }

    // 내 폴더 목록(트리) 조회
    @GetMapping("/my")
    @Operation(summary = "내 폴더 목록(트리) 조회")
    public ApiResponse<List<FolderTreeResponseDTO>> getMyFolderTree(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FolderTreeResponseDTO> folderTree = folderService.getMyFolderTree(userDetails.getUsers().getId());
        return ApiResponse.of(SuccessStatus._FOLDER_OK, folderTree);
    }

    // 중분류 폴더 목록 조회
    @GetMapping("/parentFolders")
    @Operation(summary = "중분류 폴더 조회")
    public ApiResponse<List<FolderListResponseDTO>> getParentFolderList(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FolderListResponseDTO> folderList = folderService.getParentFolders(userDetails.getUsers().getId());
        return ApiResponse.of(SuccessStatus._FOLDER_PARENT_OK, folderList);
    }

    // (중분류) 하위 폴더 목록 조회
    @GetMapping("/{parentFolderId}/subfolders")
    @Operation(summary = "중분류 내부의 하위 폴더 조회")
    public ApiResponse<List<FolderListResponseDTO>> getSubFolderList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long parentFolderId
    ) {
        List<FolderListResponseDTO> folderList = folderService.getSubFolders(userDetails.getUsers().getId(), parentFolderId);
        return ApiResponse.of(SuccessStatus._FOLDER_SUBFOLDER_OK, folderList);
    }

    // 북마크 설정/해제
    @PatchMapping("/{folderId}/bookmark")
    @Operation(summary = "북마크 설정/해제")
    public ApiResponse<BookmarkUpdateResponseDTO> updateBookmark(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @RequestBody BookmarkUpdateRequestDTO request
    ) {
        BookmarkUpdateResponseDTO response = folderService.updateBookmark(
                userDetails.getUsers().getId(), folderId, request.getIsBookmarked()
        );
        return ApiResponse.of(SuccessStatus._FOLDER_BOOKMARK_OK, response);
    }

    // 폴더 내부 링크, 폴더 목록 조회
    @GetMapping("/{folderId}/linkus")
    @Operation(summary = "폴더 내부 링크, 폴더 목록 조회")
    public ApiResponse<FolderLinkusResponseDTO> getFolderLinkus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String cursor
    ) {
        FolderLinkusResponseDTO response = folderService.getFolderLinkus(
                userDetails.getUsers().getId(), folderId, limit, cursor);
        return ApiResponse.of(SuccessStatus._FOLDER_LINK_OK, response);
    }
}
