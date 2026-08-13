package com.umc.linkyou.web.dto.linku;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;

public class LinkuResponseDTO {

    @Builder
    public record LinkuResultDTO(
            Long userId, Long userLinkuId, String folderName, Long categoryId, String linku, String memo,
            Long emotionId, Long situationId, Boolean isEmotionAi, Boolean isSituationAi, String domain,
            String title, String domainImageUrl, String linkuImageUrl, Boolean aiArticleExists,
            LocalDateTime createdAt, LocalDateTime updatedAt, String keyword, String summary
    ) {
        public LinkuResultDTO {
            aiArticleExists = aiArticleExists != null ? aiArticleExists : false;
        }
    }

    @Builder
    public record LinkuFolderChangeResultDTO(
            Long userLinkuId, Long folderId, String folderName, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {}

    @Builder
    public record LinkuSimpleDTO(
            Long userLinkuId, Long categoryId, String folderName, String linku, String memo, Long emotionId,
            String title, String domain, String domainImageUrl, String linkuImageUrl, Boolean aiArticleExists,
            LocalDateTime lastViewedAt
    ) {
        public LinkuSimpleDTO {
            aiArticleExists = aiArticleExists != null ? aiArticleExists : false;
        }
    }

    @Builder
    public record LinkuIsExistDTO(
            boolean isExist, Long userId, String title, String memo, Long emotionId,
            LocalDateTime createdAt, LocalDateTime updatedAt
    ) {}

    @Builder
    public record LinkuCreateResult(LinkuResultDTO data, boolean validUrl) {}

    @Builder
    public record LinkuSliceResultDTO(List<AiArticleSummaryDTO> linkuList, String nextCursor, Boolean hasNext) {}

    @Builder
    public record AiArticleSummaryDTO(
            String linku, Long emotionId, String domain, String domainImageUrl, String title,
            String linkuImageUrl, Long categoryId, String categoryName
    ) {}

    @Builder
    public record LinkuRecommendCursorPageDTO(List<LinkuSimpleDTO> items, String nextCursor, Boolean hasNext) {}
}
