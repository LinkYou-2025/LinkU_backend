package com.umc.linkyou.service.folder;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.converter.FolderConverter;
import com.umc.linkyou.apiPayload.code.status.folder.FolderErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.classification.CategoryRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.service.alarm.event.FolderDeletedAlarmEvent;
import com.umc.linkyou.web.dto.folder.*;
import com.umc.linkyou.web.dto.folder.linku.FolderLinkusResponseDTO;
import com.umc.linkyou.web.dto.folder.linku.FolderSummaryDTO;
import com.umc.linkyou.web.dto.folder.linku.LinkuSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FolderServiceImpl implements FolderService {
    private final FolderRepository folderRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final UsersFolderRepository usersFolderRepository;
    private final LinkuFolderRepository linkuFolderRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 하위 폴더 생성
    @Transactional
    public FolderResponseDTO createFolder(Long userId, Long parentFolderId, FolderCreateRequestDTO req) {
        Folder parent = folderRepository.findById(parentFolderId).orElse(null);

        // 부모 폴더 존재 확인
        if (parent == null) {
            throw new GeneralException(FolderErrorStatus._FOLDER_PARENT_NOT_FOUND);
        }

        // 부모 폴더에 대한 생성 권한 확인 (소유자 또는 편집자만 가능)
        if (!usersFolderRepository.existsFolderOwnerOrWriter(userId, parentFolderId)) {
            throw new GeneralException(FolderErrorStatus._FOLDER_CREATE_FORBIDDEN);
        }

        // 카테고리명과 동일한 이름 사용 방지
        if (categoryRepository.existsByCategoryName(req.getFolderName())) {
            throw new GeneralException(FolderErrorStatus._FOLDER_NAME_CONFLICT);
        }

        // 해당 부모 아래 중복 이름 체크
        boolean isDuplicate = folderRepository.existsByParentIdAndName(parentFolderId, req.getFolderName());

        if (isDuplicate) {
            throw new GeneralException(FolderErrorStatus._FOLDER_CREATE_DUPLICATE);
        }

        // 폴더 테이블에 저장
        Folder folder = Folder.builder()
                .folderName(req.getFolderName())
                .category(parent.getCategory())
                .parentFolder(parent).build();
        folderRepository.save(folder);

        // 유저폴더 매핑 테이블에 저장 및 소유자 등록
        usersFolderRepository.save(UsersFolder.builder()
                .user(userRepository
                        .findById(userId)
                        .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND)))
                .folder(folder)
                .permissionType(PermissionType.OWNER)
                .isBookmarked(false)
                .build());

        return FolderResponseDTO.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .isBookmarked(false)
                .categoryId(parent.getCategory().getCategoryId())
                .categoryName(parent.getCategory().getCategoryName())
                .parentFolderId(parent.getFolderId())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }

    // 폴더 이름 수정
    @Transactional
    public FolderResponseDTO updateFolder(Long userId, Long folderId, FolderUpdateRequestDTO req) {
        // 폴더 조회
        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

        // 주인 여부 확인
        UsersFolder usersFolder = usersFolderRepository.findByUserIdAndFolderId(userId, folderId)
                .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_UPDATE_FORBIDDEN));
        if (usersFolder.getPermissionType() != PermissionType.OWNER) {
            throw new GeneralException(FolderErrorStatus._FOLDER_UPDATE_FORBIDDEN);
        }

        if (req.getFolderName() != null)
        {
            // 부모 폴더 존재 확인
            if (folder.getParentFolder() == null) {
                throw new GeneralException(FolderErrorStatus._FOLDER_PARENT_NOT_FOUND);
            }

            // 카테고리명과 동일한 이름 사용 방지
            if (categoryRepository.existsByCategoryName(req.getFolderName())) {
                throw new GeneralException(FolderErrorStatus._FOLDER_NAME_CONFLICT);
            }

            // 해당 부모 아래 중복 이름 체크
            boolean isDuplicate = folderRepository.existsByParentIdAndName(folder.getParentFolder().getFolderId(), req.getFolderName());

            if (isDuplicate) {
                throw new GeneralException(FolderErrorStatus._FOLDER_CREATE_DUPLICATE);
            }

            folder.updateFolderName(req.getFolderName());
        }

        return FolderConverter.toFolderResponseDTO(folder, usersFolder.getIsBookmarked());
    }

    // 폴더 삭제
    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        // 폴더 조회
        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

        // 주인 여부 확인
        boolean isOwner = usersFolderRepository.existsFolderOwner(userId, folderId);
        if (!isOwner) {
            throw new GeneralException(FolderErrorStatus._FOLDER_DELETE_FORBIDDEN);
        }

        // 폴더 삭제 알람에 필요
        List<Long> memberIds = usersFolderRepository.findAllParticipantsByFolderId(folderId).stream()
                .filter(uf -> uf.getPermissionType() != PermissionType.OWNER)
                .map(uf -> uf.getUser().getId())
                .toList();

        String folderName = folder.getFolderName();
        String deleterNickname = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND))
                .getNickName();

        folderRepository.delete(folder);

        if (!memberIds.isEmpty()) {
            eventPublisher.publishEvent(new FolderDeletedAlarmEvent(folderId, memberIds, deleterNickname, folderName));
        }
    }

    // 내 폴더 목록(트리) 조회
    public List<FolderTreeResponseDTO> getMyFolderTree(Long userId) {
        // 유저 모든 폴더 조회
        List<UsersFolder> allFolders = usersFolderRepository.findAllByUserId(userId);

        // folderId → isBookmarked 맵
        Map<Long, Boolean> bookmarkMap = allFolders.stream()
                .collect(Collectors.toMap(
                        uf -> uf.getFolder().getFolderId(),
                        UsersFolder::getIsBookmarked
                ));

        // parentFolderId → 자식 Folder 목록 맵
        Map<Long, List<Folder>> childMap = allFolders.stream()
                .filter(uf -> uf.getFolder().getParentFolder() != null)
                .collect(Collectors.groupingBy(
                        uf -> uf.getFolder().getParentFolder().getFolderId(),
                        Collectors.mapping(UsersFolder::getFolder, Collectors.toList())
                ));

        // 중분류부터 재귀적으로 트리 구성
        return allFolders.stream()
                .filter(uf -> uf.getFolder().getParentFolder() == null)
                .map(uf -> buildTreeFromMap(uf.getFolder(), childMap, bookmarkMap))
                .collect(Collectors.toList());
    }

    private FolderTreeResponseDTO buildTreeFromMap(Folder folder, Map<Long, List<Folder>> parentChildMap, Map<Long, Boolean> bookmarkMap) {
        List<Folder> childFolders = parentChildMap.get(folder.getFolderId());
        List<FolderTreeResponseDTO> childDTOs = (childFolders != null && !childFolders.isEmpty())
                ? childFolders.stream()
                        .map(child -> buildTreeFromMap(child, parentChildMap, bookmarkMap))
                        .collect(Collectors.toList())
                : null;

        Category category = folder.getCategory();
        return FolderTreeResponseDTO.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .isBookmarked(bookmarkMap.getOrDefault(folder.getFolderId(), false))
                .categoryId(category != null ? category.getCategoryId() : null)
                .children(childDTOs)
                .build();
    }

    // 중분류 폴더 목록 조회
    public List<FolderListResponseDTO> getParentFolders(Long userId, String sort) {
        List<UsersFolder> parentFolders = usersFolderRepository.findParentFolders(userId);

        Comparator<UsersFolder> comparator = "updatedAt".equals(sort)
                ? Comparator.comparing((UsersFolder uf) -> uf.getFolder().getUpdatedAt()).reversed()
                : Comparator.comparing(uf -> uf.getFolder().getFolderName());

        List<Long> folderIds = parentFolders.stream()
                .map(uf -> uf.getFolder().getFolderId())
                .toList();

        Set<Long> sharedFolderIds = folderIds.isEmpty()
                ? Collections.emptySet()
                : usersFolderRepository.findAllSharedFolderIdsIn(folderIds);

        return parentFolders.stream()
                .sorted(comparator)
                .map(usersFolder -> FolderListResponseDTO.builder()
                        .folderId(usersFolder.getFolder().getFolderId())
                        .folderName(usersFolder.getFolder().getFolderName())
                        .isBookmarked(usersFolder.getIsBookmarked())
                        .isSharing(sharedFolderIds.contains(usersFolder.getFolder().getFolderId()) ? "share" : "private")
                        .build())
                .collect(Collectors.toList());
    }

    // 자식 폴더 목록 조회
    public List<FolderListResponseDTO> getSubFolders(Long userId, Long parentFolderId) {
        List<Folder> subFolders = usersFolderRepository.findAllByUserIdAndParentFolderId(userId, parentFolderId);

        if (subFolders.isEmpty()) return Collections.emptyList();

        List<Long> subFolderIds = subFolders.stream().map(Folder::getFolderId).toList();

        // 해당 하위 폴더들의 북마크 상태만 조회 (전체 조회 대신 최적화)
        Map<Long, Boolean> bookmarkMap = usersFolderRepository.findAllByUserIdAndFolderIdIn(userId, subFolderIds).stream()
                .collect(Collectors.toMap(
                        uf -> uf.getFolder().getFolderId(),
                        UsersFolder::getIsBookmarked
                ));

        // 공유 중인 폴더 ID 일괄 조회 (N+1 제거)
        Set<Long> sharedFolderIds = usersFolderRepository.findAllSharedFolderIdsIn(subFolderIds);

        return subFolders.stream()
                .map(folder -> FolderListResponseDTO.builder()
                        .folderId(folder.getFolderId())
                        .folderName(folder.getFolderName())
                        .parentFolderId(parentFolderId)
                        .isBookmarked(bookmarkMap.getOrDefault(folder.getFolderId(), Boolean.FALSE))
                        .isSharing(sharedFolderIds.contains(folder.getFolderId()) ? "share" : "private")
                        .build())
                .collect(Collectors.toList());
    }

    // 북마크 설정/해제
    @Transactional
    public BookmarkUpdateResponseDTO updateBookmark(Long userId, Long folderId, Boolean isBookmarked) {
        UsersFolder usersFolder = usersFolderRepository.findByUserIdAndFolderId(userId, folderId).orElseThrow(() -> new GeneralException(ErrorStatus._FOLDER_BOOKMARK_NOT_FOUND));

        usersFolder.updateBookmark(isBookmarked);

        return BookmarkUpdateResponseDTO.builder()
                .folderId(usersFolder.getFolder().getFolderId())
                .isBookmarked(usersFolder.getIsBookmarked())
                .build();
    }

    @Transactional(readOnly = true)
    // 폴더 내부 링크, 폴더 목록 조회
    public FolderLinkusResponseDTO getFolderLinkus(Long userId, Long folderId, int limit, String cursor, String sort) {
        // check folder exist
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new GeneralException(FolderErrorStatus._FOLDER_NOT_FOUND));

        // 접근 권한 확인 (소유자 또는 활성 공유 멤버)
        boolean hasAccess = usersFolderRepository.existsFolderOwner(userId, folderId)
                || usersFolderRepository.existsActiveMember(userId, folderId);
        if (!hasAccess) {
            throw new GeneralException(FolderErrorStatus._FOLDER_ACCESS_FORBIDDEN);
        }

        // 정렬 기준: "updatedAt" → 최근 수정순, 그 외 → 가나다순
        Sort folderSort = "updatedAt".equals(sort)
                ? Sort.by(Sort.Direction.DESC, "updatedAt")
                : Sort.by(Sort.Direction.ASC, "folderName");

        // 소분류 폴더 목록 조회
        List<Folder> subFolders = folderRepository.findAllByParentFolderId(folderId, folderSort);
        List<Long> subFolderIds = subFolders.stream().map(Folder::getFolderId).toList();

        // 현재 페이지의 폴더들에 대해 북마크 상태 및 공유 여부 일괄 조회 (빈 리스트면 DB 호출 생략)
        Map<Long, Boolean> bookmarkMap = subFolderIds.isEmpty()
                ? Collections.emptyMap()
                : usersFolderRepository.findAllByUserIdAndFolderIdIn(userId, subFolderIds).stream()
                        .collect(Collectors.toMap(
                                uf -> uf.getFolder().getFolderId(),
                                UsersFolder::getIsBookmarked
                        ));

        Set<Long> sharedFolderIds = subFolderIds.isEmpty()
                ? Collections.emptySet()
                : usersFolderRepository.findAllSharedFolderIdsIn(subFolderIds);

        // 하위 폴더 DTO 변환
        List<FolderSummaryDTO> subfolderDtos = subFolders.stream()
                .map(f -> {
                    FolderSummaryDTO dto = new FolderSummaryDTO();
                    dto.setFolderId(f.getFolderId());
                    dto.setFolderName(f.getFolderName());
                    dto.setIsBookmarked(bookmarkMap.getOrDefault(f.getFolderId(), false));
                    dto.setIsSharing(sharedFolderIds.contains(f.getFolderId()) ? "share" : "private");
                    return dto;
                }).toList();

        // 커서: 없으면 Long.MAX_VALUE, 숫자가 아니면 400 반환
        Long cursorId;
        if (cursor == null) {
            cursorId = Long.MAX_VALUE;
        } else {
            try {
                cursorId = Long.parseLong(cursor);
            } catch (NumberFormatException e) {
                throw new GeneralException(FolderErrorStatus._FOLDER_INVALID_CURSOR);
            }
        }

        // DB에서 limit + 1개를 가져옴
        PageRequest pageRequest = PageRequest.of(0, limit + 1);
        List<LinkuFolder> linkuFolders = linkuFolderRepository.findWithCursor(folderId, cursorId, pageRequest);

        // 다음 커서 계산
        boolean hasNext = linkuFolders.size() > limit;
        List<LinkuFolder> resultList = hasNext ? linkuFolders.subList(0, limit) : linkuFolders;

        String nextCursor = hasNext
                ? String.valueOf(resultList.get(resultList.size() - 1).getUsersLinku().getLinku().getLinkuId())
                : null;

        List<LinkuSummaryDTO> linkDtos = resultList.stream().map(lf -> {
            UsersLinku usersLinku = lf.getUsersLinku();
            Linku link = usersLinku.getLinku();

            LinkuSummaryDTO dto = new LinkuSummaryDTO();
            dto.setUserLinkuId(usersLinku.getUserLinkuId());
            dto.setLinkuId(link.getLinkuId());
            dto.setTitle(link.getTitle());
            dto.setUrl(link.getLinkuUrl());
            String kw = link.getLinkuKeywords().stream()
                    .map(lk -> lk.getKeyword().getName())
                    .collect(Collectors.joining(", "));
            dto.setKeyword(kw.isEmpty() ? null : kw);
            dto.setLinkuImageUrl(usersLinku.getImageUrl() != null ? usersLinku.getImageUrl() : link.getImgUrl());
            dto.setCreatedAt(link.getCreatedAt().toString());
            return dto;
        }).toList();

        FolderLinkusResponseDTO resp = new FolderLinkusResponseDTO();
        resp.setFolders(subfolderDtos);
        resp.setLinks(linkDtos);
        resp.setNextCursor(nextCursor);

        return resp;
    }

}
