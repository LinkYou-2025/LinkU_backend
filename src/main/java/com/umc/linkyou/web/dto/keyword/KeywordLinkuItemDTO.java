package com.umc.linkyou.web.dto.keyword;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

// 키워드로 필터링한 내 링크 카드 1건
public record KeywordLinkuItemDTO(
        @Schema(description = "사용자링크 ID (커서로 사용)")
        Long userLinkuId,
        @Schema(description = "표시 제목")
        String title,
        @Schema(description = "링크 URL")
        String url,
        @Schema(description = "링크 대표 이미지 URL")
        String imageUrl,
        @Schema(description = "도메인 이름", example = "example.com")
        String domain,
        @Schema(description = "도메인 아이콘 이미지 URL")
        String domainImageUrl,
        @Schema(description = "카테고리 + 감정 태그 (링크의 카테고리, 저장 시 남긴 감정)", example = "[\"라이프스타일\", \"평온\"]")
        List<String> categories
) {}
