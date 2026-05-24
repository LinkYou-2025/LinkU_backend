package com.umc.linkyou.service.folder.share;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.FolderShareLink;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.FolderShareLinkRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.folder.share.InvitationInfoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvitationServiceImpl implements InvitationService {

    private final FolderShareLinkRepository folderShareLinkRepository;
    private final UsersFolderRepository usersFolderRepository;
    private final UserRepository userRepository;

    // 초대 정보 조회
    public InvitationInfoResponseDTO getInvitationInfo(String token) {
        FolderShareLink link = folderShareLinkRepository.findByToken(token)
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVITATION_NOT_FOUND));

        if (!link.isValid()) {
            throw new GeneralException(ErrorStatus.INVITATION_EXPIRED);
        }

        return InvitationInfoResponseDTO.builder()
                .folderName(link.getFolder().getFolderName())
                .ownerName(link.getCreator().getNickName())
                .build();
    }

    // 초대 수락
    @Transactional
    public Long acceptInvitation(Long userId, String token) {
        // 토큰 유효성 검증
        FolderShareLink link = folderShareLinkRepository.findByToken(token)
                .orElseThrow(() -> new GeneralException(ErrorStatus.INVITATION_NOT_FOUND));

        if (!link.isValid()) {
            throw new GeneralException(ErrorStatus.INVITATION_EXPIRED);
        }

        Folder folder = link.getFolder();
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        //Owner caanot accpet its own
        if (link.getCreator().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus.INVITATION_CREATOR_CANNOT_ACCEPT);
            // 또는 기존: ErrorStatus._FOLDER_PERMISSION_NOT_ALLOWED
        }
        // 이미 참여 중인지 확인
        boolean isAlreadyMember = usersFolderRepository.existsActiveMember(userId, folder.getFolderId());
        if (isAlreadyMember) {
            // 이미 멤버라면 폴더 ID만 반환
            return folder.getFolderId();
        }

        // 멤버 추가
        UsersFolder newMember = UsersFolder.builder()
                .user(user)
                .folder(folder)
                .permissionType(PermissionType.VIEWER)
                .isBookmarked(false)
                .build();

        usersFolderRepository.save(newMember);

        return folder.getFolderId();
    }
}