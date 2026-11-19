package com.umc.linkyou.repository.categoryRepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;

public interface UsersCategoryColorRepository
        extends JpaRepository<UsersCategoryColor, Long>, UsersCategoryColorRepositoryCustom {
    List<UsersCategoryColor> findByUserId(Long userId);
}
