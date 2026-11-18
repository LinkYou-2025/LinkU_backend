package com.umc.linkyou.infra.ai.dto;

import com.umc.linkyou.domain.enums.PageType;

// pageType: 요약과 같은 호출에서 받은 페이지 종류. 비콘텐츠 재검증 시 별도 판별 호출 없이 이 값을 쓴다.
public record AiArticleResultDTO(String summary, String pageType) {

    // pageType을 쓰지 않는 기존 호출부/테스트 호환용
    public AiArticleResultDTO(String summary) {
        this(summary, null);
    }

    public PageType resolvedPageType() {
        return PageType.from(pageType);
    }
}
