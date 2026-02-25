package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.service.category.CategoryService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.category.CategoryListResponseDTO;
import com.umc.linkyou.web.dto.category.UpdateCategoryColorRequestDTO;
import com.umc.linkyou.web.dto.category.UserCategoryColorResponseDTO;
import com.umc.linkyou.web.dto.folder.FolderUpdateRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<CategoryListResponseDTO> categoryList = categoryService.getCategories(userDetails.getUsers().getId());
        return ApiResponse.of(SuccessStatus._CATEGORY_OK, categoryList);
    }

    @Operation(
            summary = "유저 카테고리(중분류 폴더) 색상 수정",
            description = "사용자의 카테고리(중분류 폴더) 색상을 수정합니다."
    )
    @PutMapping("/{categoryId}/color")
    public ApiResponse<UserCategoryColorResponseDTO> updateUserCategoryColor(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long categoryId,
            @RequestBody UpdateCategoryColorRequestDTO request
    ) {
        UserCategoryColorResponseDTO userCategoryColor =
                categoryService.updateUserCategoryColor(userDetails.getUsers().getId(), categoryId, request);
        return ApiResponse.of(SuccessStatus._CATEGORY_COLOR_OK, userCategoryColor);
    }
}
