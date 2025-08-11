package com.umc.linkyou.service.folder.shared;

import com.umc.linkyou.converter.FolderConverter;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.FolderRepository;
import com.umc.linkyou.repository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.service.folder.share.ShareFolderService;
import com.umc.linkyou.web.dto.folder.FolderListResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderTreeResponseDTO;
import com.umc.linkyou.web.dto.folder.share.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SharedFolderServiceImpl implements SharedFolderService {
    private final UsersFolderRepository usersFolderRepository;
    private final FolderConverter folderConverter;

    // 공유받은 폴더 트리 조회
    public List<SharedFolderTreeResponseDTO> getSharedFolderTree(Long userId) {
        List<Folder> sharedFolders = usersFolderRepository.findSharedFolders(userId);

        // 공유자 유저id별 그룹핑
        Map<Long, List<Folder>> userIdFolderMap = sharedFolders.stream()
                .collect(Collectors.groupingBy(folder -> findOwnerId(folder.getFolderId())));

        List<SharedFolderTreeResponseDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<Folder>> entry : userIdFolderMap.entrySet()) {
            Long ownerId = entry.getKey();
            List<Folder> folders = entry.getValue();

            String nickname = findOwnerNickname(folders.get(0).getFolderId());

            // 트리 변환
            Map<Long, List<Folder>> parentChildMap = folders.stream()
                    .collect(Collectors.groupingBy(folder ->
                            folder.getParentFolder() != null ? folder.getParentFolder().getFolderId() : 0L));

            List<FolderTreeResponseDTO> folderTrees = parentChildMap.getOrDefault(0L, new ArrayList<>()).stream()
                    .map(rootFolder -> buildTreeFromMap(rootFolder, parentChildMap, userId))
                    .collect(Collectors.toList());

            SharedFolderTreeResponseDTO dto = SharedFolderTreeResponseDTO.builder()
                    .userId(ownerId)
                    .nickname(nickname)
                    .folders(folderTrees)
                    .build();

            result.add(dto);
        }

        return result;
    }

    // 공유자 userId 조회
    public Long findOwnerId(Long folderId) {
        return usersFolderRepository.findOwnerByFolderId(folderId)
                .map(userFolder -> userFolder.getUser().getId())
                .orElse(null);
    }

    // 공유자 닉네임 조회
    public String findOwnerNickname(Long folderId) {
        return usersFolderRepository.findOwnerByFolderId(folderId)
                .map(userFolder -> userFolder.getUser().getNickName())
                .orElse("닉네임 없음");
    }

    // 폴더 트리
    private FolderTreeResponseDTO buildTreeFromMap(Folder folder, Map<Long, List<Folder>> parentChildMap, Long userId) {
        FolderTreeResponseDTO dto = folderConverter.toFolderTreeDTO(folder, userId);

        List<Folder> childFolders = parentChildMap.get(folder.getFolderId());
        if (childFolders != null && !childFolders.isEmpty()) {
            List<FolderTreeResponseDTO> childDTOs = childFolders.stream()
                    .map(child -> buildTreeFromMap(child, parentChildMap, userId))
                    .collect(Collectors.toList());
            dto.setChildren(childDTOs);
        } else {
            dto.setChildren(null);
        }
        return dto;
    }

    public List<FolderListResponseDTO> getSharedFolders(Long userId) {
        // 유저 폴더 테이블에서 isOwner가 false고 isViewer가 true인 폴더들 조회
        List<Folder> folders = usersFolderRepository.findSharedFolders(userId);

        return folders.stream()
                .map(folder -> FolderListResponseDTO.builder()
                        .folderId(folder.getFolderId())
                        .folderName(folder.getFolderName())
                        .build())
                .collect(Collectors.toList());
    }

    // 공유 받은 폴더 삭제
    public FolderResponseDTO deleteSharedFolder(Long userId, Long folderId) {
        // 폴더 조회
        UsersFolder usersFolder = usersFolderRepository
                .findByUserIdAndFolderId(userId, folderId)
                .orElseThrow(() -> new AccessDeniedException("공유 폴더가 없습니다."));

        // 유저 폴더 테이블에서 삭제
        usersFolderRepository.delete(usersFolder);

        Folder folder = usersFolder.getFolder();
        return folderConverter.toFolderResponseDTO(folder);
    }
}

