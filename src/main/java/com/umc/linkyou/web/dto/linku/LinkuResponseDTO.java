package com.umc.linkyou.web.dto.linku;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class LinkuResponseDTO {
    @Setter
    @Getter
    @Builder
    public static class LinkuResultDTO {
        private Long userId;
        private Long userLinkuId;
        private Long linkuId;
        private Long linkuFolderId;
        private Long categoryId;
        private String linku; //링크
        private String memo;
        private Long emotionId;
        private Long situationId;
        private Boolean isEmotionAi;
        private Boolean isSituationAi;
        private String domain;
        private String title;
        private String domainImageUrl;
        private String linkuImageUrl;
        @Builder.Default
        private Boolean aiArticleExists = false;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String keyword;
        private String summary;
    }
    @Getter
    @Setter
    @Builder
    public static class LinkuSimpleDTO {
        private Long linkuId;
        private Long categoryId;
        private String linku;
        private String memo;
        private Long emotionId;
        private String title;
        private String domain;
        private String domainImageUrl;
        private String linkuImageUrl;
        @Builder.Default
        private Boolean aiArticleExists = false;
        private LocalDateTime lastViewedAt;
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

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkuSliceResultDTO {
        private List<LinkuResultDTO> linkuList;
        private String nextCursor;
        private Boolean hasNext;
    }
}
