package com.umc.linkyou.repository.mapping.linkuFolderRepository;

import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface LinkuFolderRepository  extends JpaRepository<LinkuFolder, Long>, LinkuFolderRepositoryCustom {
    Optional<Object> findById(long l);

    Optional<LinkuFolder>  findFirstByUsersLinku_UserLinkuIdOrderByLinkuFolderIdDesc(Long userLinkuId);

    List<LinkuFolder> findByUsersLinku(UsersLinku usersLinku);
}
