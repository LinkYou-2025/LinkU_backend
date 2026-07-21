package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.InvitationErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.ShareFolderErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.folder.share.FolderPermissionRequestDTO;
import com.umc.linkyou.web.dto.folder.share.MySharedFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ViewerResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "share-folder-controller", description = "폴더 공유 관련 API")
@RequestMapping("/folders/share")
public interface ShareFolderApi {

    @Operation(summary = "초대 링크 생성", description = "해당 폴더의 초대용 토큰을 생성합니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND}, shareFolderErrorStatus = {ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED})
    @PostMapping("/{folderId}/invitation")
    ApiResponse<String> createInviteLink(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    );

    @Operation(summary = "초대 링크 삭제", description = "초대 링크를 비활성화합니다.")
    @ApiErrorCode(shareFolderErrorStatus = {ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED}, invitationErrorStatus = {InvitationErrorStatus.INVITATION_LINK_NOT_FOUND})
    @DeleteMapping("/{folderId}/invitation")
    ApiResponse<Object> deactivateInviteLink(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    );

    @Operation(summary = "폴더 멤버 조회", description = "공유된 폴더의 멤버 목록을 조회합니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND}, shareFolderErrorStatus = {ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED})
    @GetMapping("/{folderId}/members")
    ApiResponse<List<ViewerResponseDTO>> getFolderViewers(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    );

    @Operation(summary = "폴더 권한 수정", description = "폴더 공유 멤버의 권한(뷰어/라이터)을 수정합니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND}, shareFolderErrorStatus = {ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_FOUND, ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED, ShareFolderErrorStatus._FOLDER_OWNER_UPDATE_NOT_ALLOWED, ShareFolderErrorStatus._INVALID_PERMISSION_TYPE})
    @PutMapping("/{folderId}/members/{userFolderId}")
    ApiResponse<ShareFolderResponseDTO> updateViewerPermission(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId,
            @PathVariable Long userFolderId,
            @Valid @RequestBody FolderPermissionRequestDTO request
    );

    @Operation(summary = "폴더 비공개 전환", description = "공유된 폴더를 비공개로 전환하고 모든 공유 권한을 제거합니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND}, shareFolderErrorStatus = {ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED})
    @PostMapping("/{folderId}/unshare")
    ApiResponse<ShareFolderResponseDTO> unshareFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    );

    @Operation(summary = "내 폴더 나가기", description = "가장 오래 참여한 멤버에게 폴더 소유권을 자동으로 위임하고 본인은 폴더에서 나갑니다.")
    @ApiErrorCode(folderErrorStatus = {FolderErrorStatus._FOLDER_NOT_FOUND}, shareFolderErrorStatus = {ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED, ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_FOUND, ShareFolderErrorStatus._FOLDER_LEAVE_NO_MEMBER_TO_TRANSFER})
    @PostMapping("/{folderId}/leave")
    ApiResponse<ShareFolderResponseDTO> leaveFolder(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long folderId
    );

    @Operation(summary = "내가 공유한 폴더 목록 조회", description = "내가 소유자이면서 실제로 참여 중인 멤버(뷰어/편집자)가 있는 폴더 목록을 조회합니다. 초대 링크만 활성화되어 있고 아직 아무도 참여하지 않은 폴더는 제외됩니다.")
    @GetMapping("/my")
    ApiResponse<List<MySharedFolderResponseDTO>> getMySharedFolders(
            @CurrentUser CustomUserDetails userDetails
    );
}
