package com.umc.linkyou.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

public class DomainDTO {

    /**
     * 도메인 생성/수정 요청 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomainRequestDTO {

        @Schema(example = "1", description = "도메인 id (수정 시 필수)")
        private Long id;

        @Schema(example = "NAVER", description = "도메인 이름")
        private String name;

        @Schema(example = "naver.com", description = "도메인 값")
        private String domainTail;
    }

    /**
     * 단건 응답 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomainReponseDTO {
        private String name;
        private String domainTail;
        private String imageUrl;
    }

    /**
     * 커서 페이징 응답 DTO
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DomainCursorPageResponse {
        private List<DomainReponseDTO> items; // Domain 목록
        private Long nextCursor;              // 다음 요청 시작 ID
        private boolean hasNext;              // 다음 페이지 존재 여부
    }
}
