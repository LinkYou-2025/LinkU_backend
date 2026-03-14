package com.umc.linkyou.service.folder;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.web.dto.folder.*;
import com.umc.linkyou.web.dto.folder.linku.FolderLinkusResponseDTO;

import java.util.List;

public interface FolderService {
    FolderResponseDTO createFolder(Long userId, Long parentFolderId, FolderCreateRequestDTO req);

    FolderResponseDTO updateFolder(Long userId, Long folderId, FolderUpdateRequestDTO req);

    void deleteFolder(Long userId, Long folderId);

    // 폴더 트리 전체 조회
    List<FolderTreeResponseDTO> getMyFolderTree(Long userId);

    // 중분류 폴더 목록 조회
    List<FolderListResponseDTO> getParentFolders(Long userId, String sort);

    // 자식 폴더 목록 조회
    List<FolderListResponseDTO> getSubFolders(Long userId, Long parentFolderId);

    // 북마크 설정/해제
    BookmarkUpdateResponseDTO updateBookmark(Long userId, Long folderId, Boolean isBookmarked);

    // 폴더 내부 링크, 폴더 목록 조회
    FolderLinkusResponseDTO getFolderLinkus(Long userId, Long folderId, int limit, String cursor, String sort);

    // 유저의 카테고리에 해당하는 중분류 폴더 조회
    Folder findFolder(Long userId, Category category);
}
