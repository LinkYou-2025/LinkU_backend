package com.umc.linkyou.repository.FolderRepository;

import com.umc.linkyou.domain.folder.Folder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FolderRepository extends JpaRepository<Folder, Long>, FolderRepositoryCustom {
    // 특정 폴더의 부모 폴더 조회
    @Query("""
        select f.parentFolder
        from Folder f
        where f.folderId = :folderId
        """)
    Optional<Folder> findParentFolder(@Param("folderId") Long folderId);

    // 부모 폴더 내부에 같은 이름의 폴더 있는지 조회
    @Query("""
        select count(f) > 0
        from Folder f
        where f.parentFolder.folderId = :parentId
            and f.folderName = :folderName
        """)
    boolean existsByParentIdAndName(
            @Param("parentId") Long parentId,
            @Param("folderName") String folderName
    );

    // 특정 중분류 하위 소분류 조회
    @Query("""
        select f
        from Folder f
        join fetch f.category
        where f.parentFolder.folderId = :parentFolderId
        """)
    List<Folder> findAllByParentFolderId(
            @Param("parentFolderId") Long parentFolderId,
            Sort sort
    );
}