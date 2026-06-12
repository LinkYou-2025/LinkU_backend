package com.umc.linkyou.web.dto.linku;

public record LinkuSearchSuggestionResponse(
        Long linkuId,
        String title,         // 링크 제목
        String domainImageUrl, // 도메인 로고 URL
        String linkUrl       // 실제 접속 URL
) {
}
