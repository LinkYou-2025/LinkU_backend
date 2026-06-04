package com.umc.linkyou.web.dto.linku;

public record LinkuSearchSuggestionResponse(
        Long linkuId,
        String title,
        String imageUrl,
        String linku
) {
}
