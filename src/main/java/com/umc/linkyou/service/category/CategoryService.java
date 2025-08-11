package com.umc.linkyou.service.category;

import com.umc.linkyou.web.dto.category.CategoryListResponseDTO;
import com.umc.linkyou.web.dto.category.UpdateCategoryColorRequestDTO;
import com.umc.linkyou.web.dto.category.UserCategoryColorResponseDTO;

import java.util.List;

public interface CategoryService {
    // 카테고리 목록 조회
    List<CategoryListResponseDTO> getCategories(Long userId);

    // 유저 카테고리(중분류 폴더) 색상 수정
    UserCategoryColorResponseDTO updateUserCategoryColor(Long id, Long categoryId, UpdateCategoryColorRequestDTO request);
}
