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
public class CurationDetailResponse {
    @Schema(description = "큐레이션 ID", example = "1")
    private Long curationId;

    @Schema(description = "큐레이션 기준 월", example = "2025-07")
    private String month;

    @Schema(description = "상단 멘트", example = "닉네임님, 잔잔한 평온함 속에서 잠시 숨을 고르며 나를 돌아보는 시간을 가져보세요.")
    private String headerMent;

    @Schema(description = "하단 멘트", example = "닉네임님, 당신의 평온한 하루가 더욱 깊어지기를 바라며, 링크유가 언제나 당신 곁에 있겠습니다.")
    private String footerMent;

    @Schema(description = "헤더/푸터 멘트가 모두 준비됐는지 여부", example = "true")
    private boolean mentReady;
}