package com.umc.linkyou.repository.usersFolderRepository;

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
           
    // 공유 받은 폴더 주인 찾기
    @Query(""" 
                select uf
                from UsersFolder uf
                join fetch uf.user
                where uf.folder.folderId = :folderId
                and uf.isOwner = true""")
    Optional<UsersFolder> findOwnerByFolderId(@Param("folderId") Long folderId);

    @Query("SELECT uf FROM UsersFolder uf WHERE uf.folder.folderId IN :folderIds AND uf.isOwner = true")
    List<UsersFolder> findOwnersByFolderIdIn(@Param("folderIds") List<Long> folderIds);
}
