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
public class CurationSectionResponse {
    @Schema(description = "섹션 번호", example = "1")
    private int section;

    @Schema(description = "섹션 제목", example = "2026 월간 큐레이션 6월호")
    private String title;

    @Schema(description = "섹션 설명", example = "이번달을 위한 링크, 링큐가 준비했어요.")
    private String description;

    @Schema(description = "섹션 대표 이미지 URL", example = "https://cdn.example.com/sections/2026-06-1.png")
    private String imageUrl;
}
