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
    Optional<LinkuFolder>  findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(Long userLinkuId);

    List<LinkuFolder> findByUsersLinku(UsersLinku usersLinku);

    // 여러 UsersLinku의 폴더 매핑을 한 번에 조회 (리스트 응답에서 N+1 방지용). folder도 함께 fetch join.
    @Query("""
        select lf from LinkuFolder lf
        join fetch lf.folder f
        where lf.usersLinku.userLinkuId in :userLinkuIds
        order by lf.linkuFolderId desc
    """)
    List<LinkuFolder> findByUsersLinku_UserLinkuIdIn(@Param("userLinkuIds") List<Long> userLinkuIds);

    @Query("""
        select lf from LinkuFolder lf
        join fetch lf.usersLinku ul
        join fetch ul.linku l
        left join fetch l.domain d
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
