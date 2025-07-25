package com.umc.linkyou.repository.UsersFolderRepository;

import com.umc.linkyou.domain.mapping.folder.UsersFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsersFolderRepository extends JpaRepository<UsersFolder, Long>, UsersFolderRepositoryCustom {
    @Query("select count(uf) > 0 from UsersFolder uf where uf.user.id = :userId and uf.folder.folderId = :folderId and uf.isOwner = true")
    boolean existsFolderOwner(@Param("userId") Long userId, @Param("folderId") Long folderId);
}