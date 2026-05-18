package com.umc.linkyou.infra.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CategoryResultDTO {
    private Long categoryId;
    private String keywords;
}
