package com.umc.linkyou.gemini.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SummaryResultDTO {
    private String title;
    private String summary;
    private Long categoryId;
    private String keywords;
}
