package com.umc.linkyou.gemini.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiAnalysisResult {
    private String title;
    private String summary;
    private Long situationId;
    private Long emotionId;
    private Long categoryId;
    private String keywords;
}
