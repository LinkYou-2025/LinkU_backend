package com.umc.linkyou.web.dto.category;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCategoryColorResponseDTO {
    private Long categoryId;
    private Long fcolorId;
    private String colorName;
    private String colorCode1;
    private String colorCode2;
    private String colorCode3;
    private String colorCode4;
}
