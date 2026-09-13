package com.umc.linkyou.web.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class AiArticleResponseDTO {
    @Getter
    @Builder
    public static class AiArticleResultDTO {
        private Long id;
        private Long userLinkuId;
        private Long emotionId;
        private String emotionName;
        private String categoryName;
        private String summary;
        private String imgUrl;
        private String memo;
        private String tags;
        private String title;
    }
}
