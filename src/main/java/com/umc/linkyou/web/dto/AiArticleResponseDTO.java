package com.umc.linkyou.web.dto;

public class AiArticleResponseDTO {
    public record AiArticleResultDTO(
            Long id,
            Long linkuId,
            Long emotionId,
            String emotionName,
            String categoryName,
            String summary,
            String imgUrl,
            String memo,
            String tags,
            String title
    ) {}
}
