package com.umc.linkyou.web.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "블로그 텍스트 수집 요청 객체")
public class BlogTextRequestDTO {

    @Schema(description = "수집할 블로그 URL", example = "https://blog.naver.com/example/123")
    private String url;

    @Schema(
            description = "배치 수집용 URL 리스트 (최대 100개 권장)",
            example = "[\"https://blog.naver.com/moki_allrecords/223527506604\", \"https://kimdee.tistory.com/entry/오픽-후기\", \"https://brunch.co.kr/@chlngers/512\"]"
    )
    private List<String> urls;
}
