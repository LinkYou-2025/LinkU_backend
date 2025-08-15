package com.umc.linkyou.service.folder.share;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.folder.share.FolderPermissionRequestDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderRequestDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ViewerResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ShareFolderServiceImpl implements ShareFolderService {
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final UsersFolderRepository usersFolderRepository;

    // 폴더 공유 (뷰어 권한 설정)
    public ShareFolderResponseDTO shareFolder(Long userId, Long folderId, ShareFolderRequestDTO request) {
        // 이미 뷰어 권한 갖고 있는지 검사
        UsersFolder usersFolder = usersFolderRepository
                .findByUserIdAndFolderId(request.getUserId(), folderId)
                .orElseGet(() -> UsersFolder.builder()
                        .user(userRepository.getById(request.getUserId()))
                        .folder(folderRepository.getById(folderId))
                        .build()
                );

        usersFolder.setIsViewer(true);
        usersFolder.setIsWriter(true);
        usersFolder.setIsOwner(false);
        usersFolder.setIsBookmarked(false);

        usersFolderRepository.save(usersFolder);

        return ShareFolderResponseDTO.builder()
                .folderId(folderId)
                .userId(request.getUserId())
                .permission("VIEWER")
                .sharedAt(usersFolder.getCreatedAt().toString())
                .build();
    }

    // 폴더 뷰어 조회
    public List<ViewerResponseDTO> getViewers(Long userId, Long folderId) {
        List<UsersFolder> viewers = usersFolderRepository.findByFolderFolderIdAndIsViewerTrue(folderId);

        return viewers.stream()
                .map(uf -> {
                    ViewerResponseDTO dto = new ViewerResponseDTO();
                    dto.setUserId(uf.getUser().getId());
                    dto.setUserName(uf.getUser().getNickName());
                    dto.setPermission("VIEWER");
                    return dto;
                })
                .toList();

    }

    // 유저의 폴더 권한 수정
    public ShareFolderResponseDTO updateViewerPermission(Long userId, Long folderId, Long userFolderId, FolderPermissionRequestDTO request) {
        UsersFolder usersFolder = usersFolderRepository.findById(userFolderId).orElseThrow(() -> new GeneralException(ErrorStatus._FOLDER_PERMISSION_NOT_FOUND));

        // 오너일 경우 권한 변경 불가
        if (Boolean.TRUE.equals(usersFolder.getIsOwner())) {
            throw new GeneralException(ErrorStatus.FOLDER_OWNER_UPDATE_NOT_ALLOWED);
        }

        PermissionType permission = request.getPermission();

        if (permission == PermissionType.VIEWER) {
            usersFolder.setIsWriter(false);
            usersFolder.setIsViewer(true);
        } else if (permission == PermissionType.WRITER) {
            usersFolder.setIsWriter(true);
            usersFolder.setIsViewer(true);
        } else if (permission == PermissionType.NONE){
            usersFolder.setIsWriter(false);
            usersFolder.setIsViewer(false);
        }
        usersFolderRepository.save(usersFolder);

        return ShareFolderResponseDTO.builder()
                .folderId(folderId)
                .userId(usersFolder.getUser().getId())
                .permission(permission.name())
                .sharedAt(usersFolder.getUpdatedAt().toString())
                .build();
    }

    // 폴더 비공개 전환
    public ShareFolderResponseDTO unshare(Long ownerId, Long folderId) {
        // 폴더 주인인지 확인
        boolean isOwner = usersFolderRepository
                .existsFolderOwner(ownerId, folderId);
        if (!isOwner) {
            throw new AccessDeniedException("폴더 주인만 비공개로 전환 가능");
        }

        // 뷰어들 조회
        List<UsersFolder> mappings =
                usersFolderRepository.searchViewers(folderId);

        // 권한 박탈
        mappings.forEach(uf -> {
            uf.setIsViewer(false);
            uf.setIsWriter(false);
        });

        usersFolderRepository.saveAll(mappings);

        return ShareFolderResponseDTO.builder()
                .folderId(folderId)
                .userId(ownerId)
                .permission("PRIVATE")
                .sharedAt(LocalDateTime.now().toString())
                .build();
    }
}

