package com.umc.linkyou.repository.categoryRepository;

import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsersCategoryColorRepository extends JpaRepository<UsersCategoryColor, Long>, UsersCategoryColorRepositoryCustom {
    List<UsersCategoryColor> findByUserId(Long userId);
}