package com.umc.linkyou.openApiSummary;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryAnalysisResultDTO {
    private String title;
    private String summary;
    private Long situationId;
    private Long emotionId;
    private Long categoryId;
    private String keywords;
}

