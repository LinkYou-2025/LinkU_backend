package com.umc.linkyou.repository.usersFolderRepository;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UsersFolderRepository extends JpaRepository<UsersFolder, Long>, UsersFolderRepositoryCustom {
    // 유저id랑 폴더id로 관계 엔티티 조회
    @Query("""
        select uf
        from UsersFolder uf
        where uf.user.id = :userId
            and uf.folder.folderId = :folderId
        """)
    Optional<UsersFolder> findByUserIdAndFolderId(
            @Param("userId") Long userId,
            @Param("folderId") Long folderId
    );

    // 유저가 속한 모든 폴더 관계 조회 (활성 권한이 있는 경우만)
    @Query("""
        select uf
        from UsersFolder uf
        join fetch uf.folder f
        left join fetch f.parentFolder
        where uf.user.id = :userId
            and uf.permissionType <> com.umc.linkyou.domain.enums.PermissionType.NONE
        """)
    List<UsersFolder> findAllByUserId(
            @Param("userId") Long userId
    );

    // 유저의 중분류(루트) 폴더 조회 (활성 권한이 있는 경우만)
    @Query("""
        select uf
        from UsersFolder uf
        join fetch uf.folder f
        where uf.user.id = :userId
            and f.parentFolder is null
            and uf.permissionType <> com.umc.linkyou.domain.enums.PermissionType.NONE
        """)
    List<UsersFolder> findParentFolders(
            @Param("userId") Long userId
    );

    // 특정 폴더-유저 정보 조회
    @Query("""
        select uf
        from UsersFolder uf
        where uf.user.id = :userId
            and uf.folder.folderId IN :folderIds
        """)
    List<UsersFolder> findAllByUserIdAndFolderIdIn(
            @Param("userId") Long userId,
            @Param("folderIds") List<Long> folderIds
    );

    // 유저 id랑 부모 폴더id로 찾기 (활성 권한이 있는 경우만)
    @Query("""
        select uf.folder
        from UsersFolder uf
        where uf.user.id = :userId
            and uf.folder.parentFolder.folderId = :parentFolderId
            and uf.permissionType <> com.umc.linkyou.domain.enums.PermissionType.NONE
        """)
    List<Folder> findAllByUserIdAndParentFolderId(
            @Param("userId") Long userId,
            @Param("parentFolderId") Long parentFolderId
    );

    // 폴더의 소유자 정보 조회
    @Query("""
        select uf
        from UsersFolder uf
        join fetch uf.user
        where uf.folder.folderId = :folderId
            and uf.permissionType = com.umc.linkyou.domain.enums.PermissionType.OWNER
        """)
    Optional<UsersFolder> findOwnerByFolderId(
            @Param("folderId") Long folderId
    );

    // 여러 폴더의 소유자 목록 일괄 조회
    @Query("""
        select uf
        from UsersFolder uf
        join fetch uf.user
        where uf.folder.folderId in :folderIds
            and uf.permissionType = com.umc.linkyou.domain.enums.PermissionType.OWNER
        """)
    List<UsersFolder> findOwnersByFolderIdIn(
            @Param("folderIds") List<Long> folderIds
    );

    // 폴더 소유자 여부 확인
    @Query("""
        select count(uf) > 0
        from UsersFolder uf
        where uf.user.id = :userId
            and uf.folder.folderId = :folderId
            and uf.permissionType = com.umc.linkyou.domain.enums.PermissionType.OWNER
        """)
    boolean existsFolderOwner(
            @Param("userId") Long userId,
            @Param("folderId") Long folderId
    );

    // 다른 유저에게 공유 된 폴더인지 확인 (활성 권한이 있는 멤버가 존재하는 경우만)
    @Query("""
        select DISTINCT uf.folder.folderId
        from UsersFolder uf
        where uf.folder.folderId IN :folderIds
          and uf.permissionType in (
              com.umc.linkyou.domain.enums.PermissionType.VIEWER,
              com.umc.linkyou.domain.enums.PermissionType.WRITER
          )
    """)
    Set<Long> findAllSharedFolderIdsIn(
            @Param("folderIds") List<Long> folderIds
    );

    // 공유 받은 폴더 찾기 (VIEWER 또는 WRITER 권한)
    @Query("""
        select uf.folder
        from UsersFolder uf
        where uf.user.id = :userId
            and uf.permissionType in (
                com.umc.linkyou.domain.enums.PermissionType.VIEWER,
                com.umc.linkyou.domain.enums.PermissionType.WRITER
            )
        """)
    List<Folder> findAllSharedFolders(
            @Param("userId") Long userId
    );

    // viewer and writer 찾기 except owner
    @Query("""
        select uf
        from UsersFolder uf
        join fetch uf.user
        where uf.folder.folderId = :folderId
            and uf.permissionType in (
                com.umc.linkyou.domain.enums.PermissionType.VIEWER,
                com.umc.linkyou.domain.enums.PermissionType.WRITER
            )
        """)
    List<UsersFolder> findAllParticipantsByFolderId(
            @Param("folderId") Long folderId
    );

    // 유저가 해당 폴더에 활성화된 권한(뷰어/편집자)이 있는지 확인
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select COUNT(uf) > 0
        from UsersFolder uf
        where uf.user.id = :userId
            and uf.folder.folderId = :folderId
            and uf.permissionType in (
                com.umc.linkyou.domain.enums.PermissionType.VIEWER,
                com.umc.linkyou.domain.enums.PermissionType.WRITER
            )
        """)
    boolean existsActiveMember(
            @Param("userId") Long userId,
            @Param("folderId") Long folderId
    );

    @Query("""
        select uf.folder
        from UsersFolder uf
        where uf.user.id = :userId
          and uf.folder.category = :category
          and uf.folder.parentFolder is null
        """)
    Optional<Folder> findFolderByUserIdAndCategory(
            @Param("userId") Long userId,
            @Param("category") Category category
    );
}