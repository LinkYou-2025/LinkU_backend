package com.umc.linkyou.web.dto.linku;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class LinkuSearchResponseDTO {

    public record LinkuSearchItemDTO(
            @Schema(description = "사용자링크 ID (커서로 사용)")
            Long userLinkuId,
            @Schema(description = "표시 제목")
            String title,
            @Schema(description = "링크 이미지 URL")
            String linkuImageUrl,
            @Schema(description = "링크에 붙은 태그 이름 목록")
            List<String> tags,
            @Schema(description = "도메인 아이콘 URL")
            String domainImageUrl,
            @Schema(description = "도메인명", example = "유튜브")
            String domainName
    ) {}

    public record LinkuSearchCursorPageResponse(
            @Schema(description = "검색 결과 목록")
            List<LinkuSearchItemDTO> items,
            @Schema(description = "다음 페이지 요청 시 넘길 커서 (마지막 항목의 userLinkuId) , 다음 페이지가 없으면 null")
            Long nextCursor,
            @Schema(description = "다음 페이지 존재 여부")
            boolean hasNext
    ) {}
}
