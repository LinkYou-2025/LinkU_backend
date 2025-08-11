package com.umc.linkyou.web.dto.category;

import lombok.*;

@Getter
@Setter
@Builder
public class CategoryListResponseDTO {
    private Long categoryId;
    private String categoryName;

    private String colorName;
    private String colorCode1;
    private String colorCode2;
    private String colorCode3;
    private String colorCode4;
}
