package com.umc.linkyou.web.dto.linku;

import io.swagger.v3.oas.annotations.media.Schema;

public record LinkuQuickSearchResponseDTO(
        @Schema(description = "표시 제목 (사용자 지정 제목 우선, 없으면 원본 제목)")
        String title,
        @Schema(description = "도메인 아이콘 URL")
        String domainImageUrl,
        @Schema(description = "사용자 링크 ID")
        Long userLinkuId
) {
}
