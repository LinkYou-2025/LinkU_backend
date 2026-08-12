package com.umc.linkyou.web.dto.curation;

import com.umc.linkyou.domain.enums.CurationLinkuType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class RecommendedLinkResponse {
    @Schema(description = "저장된 링크 ID (사용자가 저장하지 않은 외부 추천이면 null)", example = "1")
    private Long userLinkuId;

    @Schema(description = "링크 제목", example = "퇴근 후 30분 홈트 루틴 추천")
    private String title;

    @Schema(description = "링크 URL", example = "https://example.com/articles/home-workout")
    private String url;

    @Schema(description = "링크 대표 이미지 URL", example = "https://cdn.example.com/links/home-workout.png")
    private String imageUrl;

    @Schema(description = "도메인 이름", example = "example.com")
    private String domain;

    @Schema(description = "도메인 아이콘 이미지 URL", example = "https://cdn.example.com/domains/example.png")
    private String domainImageUrl;

    @Schema(description = "카테고리 목록 (내부 추천만 포함, 외부 추천이면 null)", example = "[\"라이프스타일\"]")
    private List<String> categories;

    @Schema(description = "추천 종류 (INTERNAL: 내가 저장한 링크 기반, EXTERNAL: AI 웹 검색 기반)", example = "INTERNAL")
    private CurationLinkuType type;
}