package com.umc.linkyou.infra.ai.dto;

public record LinkuResultDTO(
        Long categoryId,
        String keywords,
        String title,
        Long situationId,
        Long emotionId
) {}
