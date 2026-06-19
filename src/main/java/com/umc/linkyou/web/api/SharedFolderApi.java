package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.ShareFolderErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.folder.share.SharedFolderGroupResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "shared-folder-controller", description = "공유 받은 폴더 관련 API")
@RequestMapping("/folders/shared")
public interface SharedFolderApi {

    @Operation(summary = "공유 받은 폴더 목록 조회", description = "사용자가 공유 받은 폴더를 소유자별로 그룹핑하여 조회합니다.")
    @GetMapping
    ApiResponse<List<SharedFolderGroupResponseDTO>> getSharedFolders(
            @CurrentUser CustomUserDetails userDetails
    );

    @Operation(summary = "공유 받은 폴더 삭제", description = "공유 받은 폴더를 자신의 목록에서 제거합니다. (폴더 자체는 삭제되지 않습니다)")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND}, shareFolderErrorStatus = {ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_FOUND})
    @DeleteMapping("/{folderId}")
    ApiResponse<Object> deleteSharedFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    );
}
