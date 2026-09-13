package com.umc.linkyou.web.dto.curation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CurationListResponse {
    @Schema(description = "큐레이션 ID (생성되지 않은 달이면 null)", example = "1")
    private Long curationId;

    @Schema(description = "큐레이션 기준 월", example = "2025-07")
    private String month;

    @Schema(description = "썸네일 이미지 URL (섹션 1 대표 이미지, 생성되지 않은 달이면 null)", example = "https://d3f9zmi4jicqrs.cloudfront.net/curation/section01/07.png")
    private String thumbnailUrl;
}