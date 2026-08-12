package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.category.CategoryErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.category.CategoryListResponseDTO;
import com.umc.linkyou.web.dto.category.UpdateCategoryColorRequestDTO;
import com.umc.linkyou.web.dto.category.UserCategoryColorResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "category-controller", description = "카테고리 관련 API")
@RequestMapping("/categories")
public interface CategoryApi {

    @Operation(summary = "카테고리 목록 조회", description = "사용자가 사용할 수 있는 모든 카테고리 목록을 조회합니다.")
    @GetMapping
    ApiResponse<List<CategoryListResponseDTO>> getCategoryList(
            @CurrentUser CustomUserDetails userDetails
    );

    @Operation(summary = "유저 카테고리(중분류 폴더) 색상 수정", description = "사용자의 카테고리(중분류 폴더) 색상을 수정합니다.")
    @ApiErrorCode(categoryErrorStatus = {CategoryErrorStatus._CATEGORY_NOT_FOUND, CategoryErrorStatus._FCOLOR_NOT_FOUND})
    @PutMapping("/{categoryId}/color")
    ApiResponse<UserCategoryColorResponseDTO> updateUserCategoryColor(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long categoryId,
            @RequestBody UpdateCategoryColorRequestDTO request
    );
}
