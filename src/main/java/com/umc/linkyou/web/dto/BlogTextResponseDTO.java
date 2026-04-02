package com.umc.linkyou.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "블로그 텍스트 수집 응답 객체")
public class BlogTextResponseDTO {

    @Schema(description = "원본 URL", example = "https://blog.naver.com/...")
    private String url;

    @Schema(description = "블로그 제목", example = "FastAPI와 KoBART 연동하기")
    private String title;

    @Schema(description = "출처 도메인", example = "blog.naver.com")
    private String domain;

    @Schema(description = "정제된 본문 텍스트 (학습용 데이터)", example = "오늘은 FastAPI를 활용해...")
    private String cleanText;

    @Schema(description = "본문 글자 수", example = "1250")
    private int textLength;

    @Schema(description = "수집 일시")
    private LocalDateTime crawledAt;
}
