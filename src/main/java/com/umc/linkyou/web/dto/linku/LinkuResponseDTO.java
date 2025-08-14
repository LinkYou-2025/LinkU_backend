package com.umc.linkyou.web.dto.linku;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

public class LinkuResponseDTO {
    @Setter
    @Getter
    @Builder
    public static class LinkuResultDTO {
        private Long userId;
        private Long linkuId;
        private Long linkuFolderId;
        private Long categoryId;
        private String linku; //링크
        private String memo;
        private Long emotionId;
        private String domain;
        private String title;
        private String domainImageUrl;
        private String linkuImageUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    @Getter
    @Setter
    @Builder
    public static class LinkuSimpleDTO {
        private Long linkuId;
        private Long categoryId;
        private String memo;
        private Long emotionId;
        private String title;
        private String domain;
        private String domainImageUrl;
        private String linkuImageUrl;
        private boolean aiArticleExists;
    }
    @Setter
    @Getter
    @Builder
    public static class LinkuIsExistDTO {
        private boolean isExist;
        private Long userId;
        private Long linkuId;
        private String title;
        private String memo;
        private Long emotionId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    @Setter
    @Getter
    @Builder
    public static class LinkuCreateResult {
        private LinkuResponseDTO.LinkuResultDTO data;
        private boolean validUrl;
    }
}
