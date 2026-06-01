package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.category.CategorySuccessStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.category.CategoryService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.CategoryApi;
import com.umc.linkyou.web.dto.category.CategoryListResponseDTO;
import com.umc.linkyou.web.dto.category.UpdateCategoryColorRequestDTO;
import com.umc.linkyou.web.dto.category.UserCategoryColorResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ApiV1
@RequiredArgsConstructor
public class CategoryController implements CategoryApi {

    private final CategoryService categoryService;

    @Override
    public ApiResponse<List<CategoryListResponseDTO>> getCategoryList(@CurrentUser CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(CategorySuccessStatus.CATEGORY_OK, categoryService.getCategories(userDetails.getUserId()));
    }

    @Override
    public ApiResponse<UserCategoryColorResponseDTO> updateUserCategoryColor(@CurrentUser CustomUserDetails userDetails, @PathVariable Long categoryId, @RequestBody UpdateCategoryColorRequestDTO request) {
        return ApiResponse.onSuccess(CategorySuccessStatus.CATEGORY_COLOR_OK, categoryService.updateUserCategoryColor(userDetails.getUserId(), categoryId, request));
    }
}
