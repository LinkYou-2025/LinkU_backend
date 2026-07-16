package com.umc.linkyou.web.dto.linku;

import io.swagger.v3.oas.annotations.media.Schema;

public record LinkuSearchHistoryItemDTO(
        @Schema(description = "검색 기록 ID")
        Long searchHistoryId,
        @Schema(description = "검색어")
        String keyword
) {
}
