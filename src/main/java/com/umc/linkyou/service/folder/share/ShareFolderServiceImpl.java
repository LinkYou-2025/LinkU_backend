package com.umc.linkyou.service.folder.share;

import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.folder.ShareFolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.folder.FolderShareLink;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.FolderShareLinkRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.folder.share.FolderPermissionRequestDTO;
import com.umc.linkyou.web.dto.folder.share.ShareFolderResponseDTO;
import com.umc.linkyou.web.dto.folder.share.ViewerResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ShareFolderServiceImpl implements ShareFolderService {
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final UsersFolderRepository usersFolderRepository;
    private final FolderShareLinkRepository folderShareLinkRepository;

    // 초대 링크 생성
    public String createInviteLink(Long userId, Long folderId) {
        // 폴더 존재 및 소유권 확인
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

        boolean isOwner = usersFolderRepository.existsFolderOwner(userId, folderId);

        if (!isOwner) {
            throw new GeneralException(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED);
        }

        // 이미 존재하는 링크 확인
        Optional<FolderShareLink> existingLink = folderShareLinkRepository.findByFolder_FolderIdAndIsActiveTrue(folderId);

        // 이미 링크가 있는 경우
        if (existingLink.isPresent()) {
            FolderShareLink link = existingLink.get();

            // 유효하면 기존 토큰 반환
            if (link.isValid()) {
                return link.getToken();
            }

            // 만료되었으면 갱신
            String newToken = UUID.randomUUID().toString();
            link.updateToken(newToken, LocalDateTime.now().plusDays(7));

            return newToken;
        }

        // 링크가 없는 경우 -> 새로 생성
        String token = UUID.randomUUID().toString();

        FolderShareLink newLink = FolderShareLink.builder()
                .token(token)
                .folder(folder)
                .creator(userRepository.findById(userId)
                        .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND)))
                .permissionType(PermissionType.VIEWER)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .isActive(true)
                .build();

        folderShareLinkRepository.save(newLink);

        return token;
    }

    // 초대 링크 비활성화
    public void deactivateInviteLink(Long userId, Long folderId) {
        // 폴더 주인 확인
        boolean isOwner = usersFolderRepository.existsFolderOwner(userId, folderId);
        if (!isOwner) {
            throw new GeneralException(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED);
        }

        FolderShareLink link = folderShareLinkRepository.findByFolder_FolderIdAndIsActiveTrue(folderId)
                .orElseThrow(() -> new GeneralException(ShareFolderErrorStatus.INVITATION_LINK_NOT_FOUND));

        link.deactivate();
    }

    // 폴더 viewer and writer 조회
    @Transactional(readOnly = true)
    public List<ViewerResponseDTO> getViewers(Long userId, Long folderId) {
        boolean isOwner = usersFolderRepository.existsFolderOwner(userId, folderId);
        if (!isOwner) {
            throw new GeneralException(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED);
        }

        List<UsersFolder> participants = usersFolderRepository.findAllParticipantsByFolderId(folderId);

        return participants.stream()
                .map(uf -> {
                    ViewerResponseDTO dto = new ViewerResponseDTO();
                    dto.setUserId(uf.getUser().getId());
                    dto.setUserName(uf.getUser().getNickName());
                    dto.setPermission(uf.getPermissionType().name());
                    return dto;
                })
                .toList();
    }

    // 유저의 폴더 권한 수정
    public ShareFolderResponseDTO updateViewerPermission(Long userId, Long folderId, Long userFolderId, FolderPermissionRequestDTO request) {
        // 요청자가 폴더 소유자인지 확인
        if (!usersFolderRepository.existsFolderOwner(userId, folderId)) {
            throw new GeneralException(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED);
        }

        UsersFolder usersFolder = usersFolderRepository.findById(userFolderId).orElseThrow(() -> new GeneralException(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_FOUND));

        if (!usersFolder.getFolder().getFolderId().equals(folderId)) {
            throw new GeneralException(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED);
        }

        // 오너의 권한은 변경 불가
        if (usersFolder.getPermissionType() == PermissionType.OWNER) {
            throw new GeneralException(ShareFolderErrorStatus._FOLDER_OWNER_UPDATE_NOT_ALLOWED);
        }

        PermissionType permission = request.getPermission();

        // OWNER 권한으로 변경은 허용하지 않음
        if (permission == PermissionType.OWNER) {
            throw new GeneralException(ShareFolderErrorStatus._INVALID_PERMISSION_TYPE);
        }

        usersFolder.setPermissionType(permission);
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
            throw new GeneralException(ShareFolderErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED);
        }

        // 활성화된 초대 링크가 있다면 만료
        folderShareLinkRepository.findByFolder_FolderIdAndIsActiveTrue(folderId)
                .ifPresent(FolderShareLink::deactivate);

        // member(viewer and writer) 조회
        List<UsersFolder> mappings =
                usersFolderRepository.findAllParticipantsByFolderId(folderId);

        // 권한 박탈
        mappings.forEach(uf -> uf.setPermissionType(PermissionType.NONE));

        usersFolderRepository.saveAll(mappings);

        return ShareFolderResponseDTO.builder()
                .folderId(folderId)
                .userId(ownerId)
                .permission("PRIVATE")
                .sharedAt(LocalDateTime.now().toString())
                .build();
    }
}

