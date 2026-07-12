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
    @Setter
    @Getter
    @Builder
    public static class LinkuFolderChangeResultDTO {
        private Long linkuId;
        private Long folderId; //실제 폴더 PK. 사용자 화면에 보이는 개인 폴더(=카테고리) id
        private String folderName;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @Builder
    public static class LinkuSimpleDTO {
        private Long userLinkuId;
        private Long linkuId;
        private Long categoryId;
        private Long folderId; //실제 폴더 PK (중분류/소분류 구분 없이 이 링크가 실제로 들어있는 폴더)
        private String folderName;
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
        private List<AiArticleSummaryDTO> linkuList;
        private String nextCursor;
        private Boolean hasNext;
    }

    // 마이페이지 AI 요약 링크 목록 전용 (필요한 필드만 최소로 구성)
    @Getter
    @Setter
    @Builder
    public static class AiArticleSummaryDTO {
        private Long linkuId;
        private String linku;
        private Long emotionId;
        private String domain;
        private String domainImageUrl;
        private String title;
        private String linkuImageUrl;
    }
}
