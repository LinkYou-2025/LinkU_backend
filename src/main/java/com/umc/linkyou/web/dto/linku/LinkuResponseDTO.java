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
        private String folderName;
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
        private Long categoryId;
        private String categoryName;
    }

    // 홈화면 링크 추천(GET /linku/recommend) 커서 페이징 응답.
    // nextCursor는 novelty/normal 두 버킷의 진행 상태를 base64(JSON)로 인코딩한 불투명 문자열이다 —
    // FE는 파싱하지 않고 다음 요청의 cursor 파라미터에 그대로 넘기기만 하면 된다.
    @Getter
    @Setter
    @Builder
    public static class LinkuRecommendCursorPageDTO {
        private List<LinkuSimpleDTO> items;
        private String nextCursor;
        private Boolean hasNext;
    }
}
