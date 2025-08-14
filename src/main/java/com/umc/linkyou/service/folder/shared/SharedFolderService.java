package com.umc.linkyou.service.folder.shared;

import com.umc.linkyou.web.dto.folder.FolderListResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.*;

import java.util.List;

public interface SharedFolderService {
    // 공유 받은 폴더 목록 조회
    List<SharedFolderTreeResponseDTO> getSharedFolderTree(Long userId);
    List<FolderListResponseDTO> getSharedFolders(Long userId);

    // 공유 받은 폴더 삭제
    FolderResponseDTO deleteSharedFolder(Long userId, Long folderId);
}