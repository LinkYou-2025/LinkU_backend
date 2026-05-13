package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.category.CategorySuccessStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.category.CategoryService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.category.CategoryListResponseDTO;
import com.umc.linkyou.web.dto.category.UpdateCategoryColorRequestDTO;
import com.umc.linkyou.web.dto.category.UserCategoryColorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "category-controller", description = "카테고리 관련 API")
@ApiV1
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(
            summary = "카테고리 목록 조회",
            description = "사용자가 사용할 수 있는 모든 카테고리 목록을 조회합니다."
    )
    @GetMapping
    public ApiResponse<List<CategoryListResponseDTO>> getCategoryList(
            @CurrentUser CustomUserDetails userDetails
    ) {
        List<CategoryListResponseDTO> categoryList = categoryService.getCategories(userDetails.getUserId());
        return ApiResponse.of(CategorySuccessStatus.CATEGORY_OK, categoryList);
    }

    @Operation(
            summary = "유저 카테고리(중분류 폴더) 색상 수정",
            description = "사용자의 카테고리(중분류 폴더) 색상을 수정합니다."
    )
    @PutMapping("/{categoryId}/color")
    public ApiResponse<UserCategoryColorResponseDTO> updateUserCategoryColor(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long categoryId,
            @RequestBody UpdateCategoryColorRequestDTO request
    ) {
        UserCategoryColorResponseDTO userCategoryColor =
                categoryService.updateUserCategoryColor(userDetails.getUserId(), categoryId, request);
        return ApiResponse.of(CategorySuccessStatus.CATEGORY_COLOR_OK, userCategoryColor);
    }
}
