package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.shared.SharedFolderService;
import com.umc.linkyou.web.dto.folder.FolderListResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderTreeResponseDTO;
import com.umc.linkyou.web.dto.folder.share.SharedFolderTreeResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders/shared")
@RequiredArgsConstructor
public class SharedFolderController {
    private final SharedFolderService sharedFolderService;

    // 공유 받은 폴더 트리 조회
    @GetMapping
    @Operation(summary = "공유 받은 폴더 트리 조회")
    public ApiResponse<List<SharedFolderTreeResponseDTO>> getSharedFolders(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        // 유저 폴더 테이블에서 isOwner가 false고 isViewer가 true인 폴더들 조회
        List<SharedFolderTreeResponseDTO> folderTree = sharedFolderService.getSharedFolderTree(userDetails.getUsers().getId());
        return ApiResponse.of(SuccessStatus._FOLDER_SHARED_OK, folderTree);
    }

    // 공유 받은 폴더 삭제
    @DeleteMapping("/{folderId}")
    @Operation(summary = "공유 받은 폴더 삭제")
    public ResponseEntity<FolderResponseDTO> deleteSharedFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        // 유저 폴더 테이블에서 삭제
        FolderResponseDTO response = sharedFolderService.deleteSharedFolder(userDetails.getUsers().getId(), folderId);
        return ResponseEntity.ok(response);
    }
}