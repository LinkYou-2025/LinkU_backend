package com.umc.linkyou.service.folder.shared;

import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.FolderConverter;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.folder.FolderResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderTreeResponseDTO;
import com.umc.linkyou.web.dto.folder.share.SharedFolderGroupResponseDTO;
import com.umc.linkyou.web.dto.folder.share.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharedFolderServiceImpl implements SharedFolderService {
    private final UsersFolderRepository usersFolderRepository;
    private final FolderConverter folderConverter;

    // 공유받은 폴더 목록 조회 (소유자별 그룹핑)
    public List<SharedFolderGroupResponseDTO> getSharedFolders(Long userId) {
        // 유저 id로 공유 받은 폴더 리스트
        List<Folder> sharedFolders = usersFolderRepository.findAllSharedFolders(userId);

        // 공유 폴더가 없으면 즉시 빈 결과 반환
        if (sharedFolders.isEmpty()) {
            return new ArrayList<>();
        }

        // 폴더 id만
        List<Long> folderIdList = sharedFolders.stream()
                .map(Folder::getFolderId)
                .collect(Collectors.toList());

        // 북마크 상태 일괄 조회
        Map<Long, Boolean> bookmarkMap = usersFolderRepository.findAllByUserIdAndFolderIdIn(userId, folderIdList).stream()
                .collect(Collectors.toMap(
                        uf -> uf.getFolder().getFolderId(),
                        UsersFolder::getIsBookmarked
                ));

        // 폴더 주인 찾기
        List<UsersFolder> ownerMappings = usersFolderRepository.findOwnersByFolderIdIn(folderIdList);
        Map<Long, Users> folderOwnerMap = ownerMappings.stream()
                .collect(Collectors.toMap(
                        uf -> uf.getFolder().getFolderId(),
                        uf -> uf.getUser()
                ));

        // 공유자 유저id별 그룹핑
        Map<Long, List<Folder>> userIdFolderMap = sharedFolders.stream()
                .collect(Collectors.groupingBy(folder -> {
                    Users owner = folderOwnerMap.get(folder.getFolderId());
                    if (owner == null) {
                        throw new GeneralException(FolderErrorStatus._FOLDER_OWNER_NOT_FOUND);
                    }
                    return owner.getId();
                }));

        List<SharedFolderGroupResponseDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<Folder>> entry : userIdFolderMap.entrySet()) {
            Long ownerId = entry.getKey();
            List<Folder> folders = entry.getValue();
            Users owner = folderOwnerMap.get(folders.get(0).getFolderId());
            String nickname = owner != null ? owner.getNickName() : "닉네임 없음";

            List<FolderTreeResponseDTO> folderDTOs = folders.stream()
                    .map(folder -> folderConverter.toFolderTreeDTO(folder, bookmarkMap))
                    .collect(Collectors.toList());

            SharedFolderGroupResponseDTO dto = SharedFolderGroupResponseDTO.builder()
                    .userId(ownerId)
                    .nickname(nickname)
                    .folders(folderDTOs)
                    .build();

            result.add(dto);
        }

        return result;
    }

    // 공유 받은 폴더 삭제
    @Transactional
    public void deleteSharedFolder(Long userId, Long folderId) {
        // 폴더 조회
        UsersFolder usersFolder = usersFolderRepository
                .findByUserIdAndFolderId(userId, folderId)
                .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

        // 소유자는 이 API로 삭제 불가 (소유자는 FolderController.deleteFolder 사용)
        if (usersFolder.getPermissionType() == PermissionType.OWNER) {
            throw new GeneralException(FolderErrorStatus._FOLDER_DELETE_FORBIDDEN);
        }

        // 유저 폴더 테이블에서 삭제
        usersFolderRepository.delete(usersFolder);
    }
}
