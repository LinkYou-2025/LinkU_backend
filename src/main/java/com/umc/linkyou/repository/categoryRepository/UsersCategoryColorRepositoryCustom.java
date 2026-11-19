package com.umc.linkyou.repository.categoryRepository;

import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;

public interface UsersCategoryColorRepositoryCustom {
    UsersCategoryColor searchCategoryColor(Long userId, Long categoryId);
}
