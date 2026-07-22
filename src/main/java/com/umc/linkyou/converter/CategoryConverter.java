package com.umc.linkyou.converter;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;
import com.umc.linkyou.web.dto.category.CategoryListResponseDTO;
import com.umc.linkyou.web.dto.category.UserCategoryColorResponseDTO;

public class CategoryConverter {

    public static CategoryListResponseDTO toCategoryListResponseDTO(Category category) {
        return CategoryListResponseDTO.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .build();
    }

    public static UserCategoryColorResponseDTO toUserCategoryColorResponseDTO(UsersCategoryColor ucc) {
        return UserCategoryColorResponseDTO.builder()
                .categoryId(ucc.getCategory().getCategoryId())
                .fcolorId(ucc.getFcolor().getFcolorId())
                .colorName(ucc.getFcolor().getColorName())
                .colorCode1(ucc.getFcolor().getColorCode1())
                .colorCode2(ucc.getFcolor().getColorCode2())
                .colorCode3(ucc.getFcolor().getColorCode3())
                .colorCode4(ucc.getFcolor().getColorCode4())
                .build();
    }
}
