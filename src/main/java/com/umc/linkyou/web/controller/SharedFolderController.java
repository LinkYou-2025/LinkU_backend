package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.FolderSuccessStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.folder.shared.SharedFolderService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.folder.share.SharedFolderGroupResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "shared-folder-controller", description = "공유 받은 폴더 관련 API")
@ApiV1
@RestController
@RequestMapping("/folders/shared")
@RequiredArgsConstructor
public class SharedFolderController {
    private final SharedFolderService sharedFolderService;

    @Operation(summary = "공유 받은 폴더 목록 조회", description = "사용자가 공유 받은 폴더를 소유자별로 그룹핑하여 조회합니다.")
    @GetMapping
    public ApiResponse<List<SharedFolderGroupResponseDTO>> getSharedFolders(
            @CurrentUser CustomUserDetails userDetails
    ) {
        List<SharedFolderGroupResponseDTO> result = sharedFolderService.getSharedFolders(userDetails.getUserId());
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_SHARED_OK, result);
    }

    @Operation(summary = "공유 받은 폴더 삭제", description = "공유 받은 폴더를 자신의 목록에서 제거합니다. (폴더 자체는 삭제되지 않습니다)")
    @DeleteMapping("/{folderId}")
    public ApiResponse<Object> deleteSharedFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    ) {
        sharedFolderService.deleteSharedFolder(userDetails.getUserId(), folderId);
        return ApiResponse.onSuccess(FolderSuccessStatus.FOLDER_LEAVE_OK);
    }
}
