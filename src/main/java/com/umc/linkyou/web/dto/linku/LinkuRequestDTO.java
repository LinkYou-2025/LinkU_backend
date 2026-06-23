package com.umc.linkyou.web.dto.linku;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

public class LinkuRequestDTO {
    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkuCreateDTO {
        @Schema(example = "https://draconist.clickn.co.kr/")
        private String linku;

        @Schema(example = "test용 메모입니다.")
        private String memo;

        @Schema(example = "2", description = "감정 ID (선택, 미입력 시 AI 분류값 사용)")
        private Long emotionId;

        @Schema(example = "1", description = "상황 ID (선택, 미입력 시 AI 분류값 사용)")
        private Long situationId;

        @Schema(example = "나만의 제목", description = "링크 제목 (미입력 시 AI 분석값 사용)")
        private String title;
    }

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkuUpdateDTO {
        @Schema(example = "16", description = "폴더 ID")
        private Long folderId;

        @Schema(example = "16", description = "카테고리 ID")
        private Long categoryId;

        @Schema(example = "수정된 링크", description = "수정할 URL 또는 링크 본문")
        private String linku;

        @Schema(example = "수정된 메모", description = "수정할 나만의 메모")
        private String memo;

        @Schema(example = "2", description = "감정/emotion ID")
        private Long emotionId;

        @Schema(example = "3", description = "상황/situation ID")
        private Long situationId;

        @Schema(example = "1", description = "도메인(domain) ID")
        private Long domainId;

        @Schema(example = "수정된 제목", description = "링크의 제목(TITLE)")
        private String title;
    }

}
