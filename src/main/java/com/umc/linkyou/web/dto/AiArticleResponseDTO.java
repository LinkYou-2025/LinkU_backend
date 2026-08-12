package com.umc.linkyou.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class AiArticleResponseDTO {
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiArticleResultDTO {
        private Long id;
        private Long linkuId;
        private Long emotionId;
        private String emotionName;
        private String categoryName;
        // PENDING/DONE/FAILED. PENDING이면 프론트는 계속 GET으로 폴링, FAILED면 멈추고 failReason으로 안내.
        private String status;
        // status=FAILED일 때만 채워지는 실패 사유 코드 (예: CRAWLER4031 - robots.txt 차단, 재시도해도 동일).
        private String failReason;
        private String summary;
        private String imgUrl;
        private String memo;
        private String tags;
        private String title;
    }
}
