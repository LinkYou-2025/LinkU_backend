package com.umc.linkyou.repository.usersFolderRepository;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsersFolderRepository extends JpaRepository<UsersFolder, Long>, UsersFolderRepositoryCustom {
    @Query("""
            select count(uf) > 0
            from UsersFolder uf
            where uf.user.id = :userId
                and uf.folder.folderId = :folderId
                and uf.isOwner = true
            """)
    boolean existsFolderOwner(@Param("userId") Long userId, @Param("folderId") Long folderId);

    List<UsersFolder> findByFolderFolderIdAndIsViewerTrue(Long folderId);

    Optional<UsersFolder> findByUserIdAndFolderId(Long userId, Long folderId);

    // share 폴더 찾기
    @Query("""
                select uf.folder
                from UsersFolder uf
                where uf.user.id = :userId
                  and uf.isOwner = false
                  and uf.isViewer = true
            """)
    List<Folder> findSharedFolders(@Param("userId") Long userId);

    // 뷰어 찾기, 주인 제외
    @Query("""
                select uf
                from UsersFolder uf
                where uf.folder.folderId = :folderId
                  and uf.isOwner = false
                  and uf.isViewer = true
            """)
    List<UsersFolder> searchViewers(@Param("folderId") Long folderId);

    // 중복 폴더 검사
    @Query("""
        SELECT COUNT(uf) > 0 FROM UsersFolder uf
        WHERE uf.user.id = :userId
          AND uf.folder.folderName = :folderName
          AND uf.folder.category = :category
    """)
    boolean existsUserFolderNameInCategory(
            @Param("userId") Long userId,
            @Param("folderName") String folderName,
            @Param("category") Category category
    );
}
