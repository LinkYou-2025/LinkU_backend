package com.umc.linkyou.infra.ai.dto;

public record AiArticleResultDTO(
        String title,
        String summary,
        Long situationId,
        Long emotionId,
        String keywords
) {}
