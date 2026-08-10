package com.umc.linkyou.repository.dto;

import java.time.LocalDateTime;

/** 추천 커서 조회에서 응답에 필요한 필드만 조회한 후보 */
public record RankedUsersLinku(
        Long userLinkuId,
        Long linkuId,
        Long categoryId,
        String linku,
        String memo,
        Long emotionId,
        String title,
        String domain,
        String domainImageUrl,
        String linkuImageUrl,
        Boolean aiArticleExists,
        LocalDateTime lastViewedAt,
        int scoreBucket) {}
