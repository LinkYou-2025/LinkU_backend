package com.umc.linkyou.service.folder;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.FolderConverter;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import com.umc.linkyou.repository.FolderRepository.FolderRepository;
import com.umc.linkyou.repository.UserRepository;
import com.umc.linkyou.repository.mapping.linkuFolderRepository.LinkuFolderRepository;
import com.umc.linkyou.repository.usersFolderRepository.UsersFolderRepository;
import com.umc.linkyou.web.dto.folder.*;
import com.umc.linkyou.web.dto.folder.linku.FolderLinkusResponseDTO;
import com.umc.linkyou.web.dto.folder.linku.FolderSummaryDTO;
import com.umc.linkyou.web.dto.folder.linku.LinkuSummaryDTO;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final UsersFolderRepository usersFolderRepository;
    private final LinkuFolderRepository linkuFolderRepository;
    private final FolderConverter folderConverter;

    // 폴더 생성
    @Transactional
    public FolderResponseDTO createFolder(Long userId, Long parentFolderId, FolderCreateRequestDTO req) {
        // 부모 폴더 조회
        Folder parent = folderRepository.findById(parentFolderId).orElse(null);

        // 부모 카테고리 조회
        if (parent == null) {
            throw new GeneralException(ErrorStatus._FOLDER_PARENT_NOT_FOUND);
        }
        Category category = parent.getCategory();
        if (category == null) {
            throw new GeneralException(ErrorStatus._FOLDER_CATEGORY_NOT_FOUND);
        }

        // 중복 폴더명 검사 (카테고리 내부)
        boolean exists = folderRepository.existsDuplicateFolder(
                req.getFolderName(), parent, category
        );

        if (exists) {
            throw new GeneralException(ErrorStatus._FOLDER_CREATE_DUPLICATE);
        }

        // 폴더 테이블에 저장
        Folder folder = Folder.builder()
                .folderName(req.getFolderName())
                .category(category)
                .parentFolder(parent).build();
        folderRepository.save(folder);

        // 유저폴더 매핑 테이블에 저장 및 소유자 true
        usersFolderRepository.save(UsersFolder.builder()
                .user(userRepository
                        .findById(userId)
                        .orElseThrow())
                .folder(folder)
                .isOwner(true)
                .isBookmarked(false)
                .isWriter(true)
                .isViewer(false)
                .build());

        return FolderResponseDTO.builder()
                .folderId(folder.getFolderId())
                .folderName(folder.getFolderName())
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .parentFolderId(parent.getFolderId()).build();
    }

    // 폴더 이름 수정
    @Transactional
    public FolderResponseDTO updateFolder(Long userId, Long folderId, FolderUpdateRequestDTO req) {
        // 폴더 조회
        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new GeneralException(ErrorStatus._FOLDER_NOT_FOUND));

        // 주인 여부 확인
        boolean isOwner = usersFolderRepository.existsFolderOwner(userId, folderId);
        if (!isOwner) {
            throw new GeneralException(ErrorStatus._FOLDER_UPDATE_FORBIDDEN);
        }

        if (req.getFolderName() != null) folder.setFolderName(req.getFolderName());
        folder.setUpdatedAt(LocalDateTime.now());

        return folderConverter.toFolderResponseDTO(folder);
    }

    // 폴더 삭제
    @Transactional
    public FolderResponseDTO deleteFolder(Long userId, Long folderId) {
        // 폴더 조회
        Folder folder = folderRepository.findById(folderId).orElseThrow(() -> new GeneralException(ErrorStatus._FOLDER_NOT_FOUND));

        // 주인 여부 확인
        boolean isOwner = usersFolderRepository.existsFolderOwner(userId, folderId);
        if (!isOwner) {
            throw new GeneralException(ErrorStatus._FOLDER_DELETE_FORBIDDEN);
        }

        folderRepository.delete(folder);

        return folderConverter.toFolderResponseDTO(folder);
    }

    // 내 폴더 목록(트리) 조회
    public List<FolderTreeResponseDTO> getMyFolderTree(Long userId) {
        // 유저 모든 폴더 조회
        List<Folder> allFolders = usersFolderRepository.searchFolders(userId, null, null, null, null, false);

        // 트리 구성, key = 중분류 폴더 ID, value = 하위 폴더
        Map<Long, List<Folder>> parentChildMap = allFolders.stream()
                .collect(Collectors.groupingBy(folder ->
                        folder.getParentFolder() != null ? folder.getParentFolder().getFolderId() : 0L));

        // 중분류부터 재귀적로 트리 구성
        return parentChildMap.getOrDefault(0L, new ArrayList<>()).stream()
                .map(folder -> buildTreeFromMap(folder, parentChildMap, userId))
                .collect(Collectors.toList());
    }

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

    // 중분류 폴더 목록 조회
    public List<FolderListResponseDTO> getParentFolders(Long userId) {
        List<UsersFolder> parentFolders = usersFolderRepository.findParentFolders(userId);

        return parentFolders.stream()
                .map(usersFolder -> FolderListResponseDTO.builder()
                        .folderId(usersFolder.getFolder().getFolderId())
                        .folderName(usersFolder.getFolder().getFolderName())
                        .isBookmarked(usersFolder.getIsBookmarked())
                        .build())
                .collect(Collectors.toList());
    }

    // 자식 폴더 목록 조회
    public List<FolderListResponseDTO> getSubFolders(Long userId, Long parentFolderId) {
        List<Folder> subFolders = usersFolderRepository.searchFolders(userId, null, parentFolderId, null, null, false);

        return subFolders.stream()
                .map(folder -> FolderListResponseDTO.builder()
                        .folderId(folder.getFolderId())
                        .folderName(folder.getFolderName())
                        .parentFolderId(parentFolderId)
                        .build())
                .collect(Collectors.toList());
    }

    // 북마크 설정/해제
    @Transactional
    public BookmarkUpdateResponseDTO updateBookmark(Long userId, Long folderId, Boolean isBookmarked) {
        UsersFolder usersFolder = usersFolderRepository.findByUserIdAndFolderId(userId, folderId).orElseThrow(() -> new GeneralException(ErrorStatus._FOLDER_BOOKMARK_NOT_FOUND));

        usersFolder.setIsBookmarked(isBookmarked);

        return BookmarkUpdateResponseDTO.builder()
                .folderId(usersFolder.getFolder().getFolderId())
                .isBookmarked(usersFolder.getIsBookmarked())
                .build();
    }

    // 폴더 내부 링크, 폴더 목록 조회
    public FolderLinkusResponseDTO getFolderLinkus(Long userId, Long folderId, int limit, String cursor) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._FOLDER_NOT_FOUND));

        List<Folder> subFolders = folderRepository.findByParentFolder_FolderId(folderId);
        List<FolderSummaryDTO> subfolderDtos = subFolders.stream()
                .map(f -> {
                    FolderSummaryDTO dto = new FolderSummaryDTO();
                    dto.setFolderId(f.getFolderId());
                    dto.setFolderName(f.getFolderName());
                    return dto;
                }).toList();

        // 커서 (없으면 Long.MAX_VALUE)
        Long cursorId = (cursor == null) ? Long.MAX_VALUE : Long.parseLong(cursor);

        // 폴더 내부 링크들
        List<LinkuFolder> linkuFolders = linkuFolderRepository.findByFolder(folder);
        List<Linku> linkus = linkuFolders.stream()
                .map(lf -> lf.getUsersLinku().getLinku())
                .filter(linku -> linku.getLinkuId() < cursorId)
                .sorted(Comparator.comparing(Linku::getLinkuId).reversed())
                .limit(limit)
                .toList();

        List<LinkuSummaryDTO> linkDtos = linkuFolders.stream().map(lf -> {
            Linku link = lf.getUsersLinku().getLinku();
            LinkuSummaryDTO dto = new LinkuSummaryDTO();

            // 링크
            dto.setLinkuId(link.getLinkuId());
            dto.setLinkuImageUrl(lf.getUsersLinku().getImageUrl());
            dto.setTitle(link.getTitle());
            dto.setUrl(link.getLinku());

            // 태그(키워드)
            AiArticle ai = link.getAiArticle();
            if (ai != null) {
                ai.getKeyword(); // ai 프록시 초기화
            }

            // 도메인
            Domain domain = link.getDomain();
            if (domain != null) {
                domain.getImageUrl(); // domain 프록시 초기화
            }

            dto.setCreatedAt(link.getCreatedAt().toString());
            return dto;
        }).toList();

        String newCursor = (linkus.size() == limit)
                ? String.valueOf(linkus.get(linkus.size()-1).getLinkuId())
                : null;

        FolderLinkusResponseDTO response = new FolderLinkusResponseDTO();
        response.setFolders(subfolderDtos);
        response.setLinks(linkDtos);
        response.setNextCursor(newCursor);

        return response;
    }
}
