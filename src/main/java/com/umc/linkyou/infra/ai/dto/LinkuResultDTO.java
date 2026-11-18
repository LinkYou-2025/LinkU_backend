package com.umc.linkyou.infra.ai.dto;

import com.umc.linkyou.domain.enums.PageType;

public record LinkuResultDTO(
        Long categoryId,
        String keywords,
        String title,
        Long situationId,
        Long emotionId,
        String pageType) {
    // pageType이 없는 기존 호출부와 테스트용
    public LinkuResultDTO(
            Long categoryId, String keywords, String title, Long situationId, Long emotionId) {
        this(categoryId, keywords, title, situationId, emotionId, null);
    }

    public PageType resolvedPageType() {
        return PageType.from(pageType);
    }
}
