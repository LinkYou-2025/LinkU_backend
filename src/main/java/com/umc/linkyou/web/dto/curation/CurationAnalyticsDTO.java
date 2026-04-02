package com.umc.linkyou.web.dto.curation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CurationAnalyticsDTO {

    // 2번째 화면) 워드클라우드용 (키워드 + 클릭 횟수)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KeywordCountResponse {
        private String keyword;
        private Long viewCount; // 열람 횟수
    }

    // 2번째 화면) 특정 키워드가 포함된 링크 리스트
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KeywordLinkResponse {
        private Long userLinkuId;
        private Long linkuId;
        private String title;
        private String url;
        private String domainImageUrl;
    }

    // 3번째 화면) 안 본 링크 리스트용 (간단한 링크 정보)
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnreadLinkResponse {
        private Long userLinkuId;
        private Long linkuId;
        private String title;
        private String url;
        private String domainImageUrl;
        private String createdAt; // 저장한 시간
    }
}