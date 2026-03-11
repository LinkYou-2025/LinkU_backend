package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.shared.SharedFolderService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.folder.FolderListResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderTreeResponseDTO;
import com.umc.linkyou.web.dto.folder.share.SharedFolderGroupResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "shared-folder-controller", description = "공유 받은 폴더 관련 API")
@ApiV1
@RestController
@RequestMapping("/folders/shared")
@RequiredArgsConstructor
public class SharedFolderController {
    private final SharedFolderService sharedFolderService;

    @Operation(
            summary = "공유 받은 폴더 목록 조회",
            description = "사용자가 공유 받은 폴더를 소유자별로 그룹핑하여 조회합니다."
    )
    @GetMapping
    public ApiResponse<List<SharedFolderGroupResponseDTO>> getSharedFoldersByOwner(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<SharedFolderGroupResponseDTO> result = sharedFolderService.getSharedFoldersByOwner(userDetails.getUsers().getId());
        return ApiResponse.of(SuccessStatus._FOLDER_SHARED_OK, result);
    }

    @Operation(
            summary = "공유 받은 폴더 삭제",
            description = "공유 받은 폴더를 자신의 목록에서 제거합니다. (폴더 자체는 삭제되지 않습니다)"
    )
    @DeleteMapping("/{folderId}")
    public ResponseEntity<FolderResponseDTO> deleteSharedFolder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        // 유저 폴더 테이블에서 삭제
        FolderResponseDTO response = sharedFolderService.deleteSharedFolder(userDetails.getUsers().getId(), folderId);
        return ResponseEntity.ok(response);
    }
}