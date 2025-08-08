package com.umc.linkyou.service.folder.share;

import com.umc.linkyou.web.dto.folder.FolderUpdateRequestDTO;
import com.umc.linkyou.web.dto.folder.share.FolderPermissionRequestDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderRequestDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ViewerResponseDTO;

import java.util.List;

public interface ShareFolderService {
    // 폴더 공유 (뷰어 권한 설정)
    ShareFolderResponseDTO shareFolder(Long userId, Long folderId, ShareFolderRequestDTO request);

    // 폴더 뷰어 조회
    List<ViewerResponseDTO> getViewers(Long userId, Long folderId);

    // 특정 뷰어 권한 수정
    ShareFolderResponseDTO updateViewerPermission(Long userId, Long folderId, Long userFolderId, FolderPermissionRequestDTO request);

    // 폴더 비공개 전환
    ShareFolderResponseDTO unshare(Long ownerId, Long folderId);
}