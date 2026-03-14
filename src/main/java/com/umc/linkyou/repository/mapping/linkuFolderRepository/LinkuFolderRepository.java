package com.umc.linkyou.repository.mapping.linkuFolderRepository;

import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkuFolderRepository  extends JpaRepository<LinkuFolder, Long>, LinkuFolderRepositoryCustom {
    Optional<Object> findById(long l);

    Optional<LinkuFolder>  findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(Long userLinkuId);

    List<LinkuFolder> findByUsersLinku(UsersLinku usersLinku);

    @Query("""
        select lf from LinkuFolder lf
        join fetch lf.usersLinku ul
        join fetch ul.linku l 
        where lf.folder.folderId = :folderId 
          and l.linkuId < :cursorId 
        order by l.linkuId desc
    """)
    List<LinkuFolder> findWithCursor(
            @Param("folderId") Long folderId,
            @Param("cursorId") Long cursorId,
            Pageable pageable
    );
}
