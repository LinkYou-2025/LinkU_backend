package com.umc.linkyou.converter;

import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.dto.RankedUsersLinku;
import com.umc.linkyou.web.dto.folder.linku.LinkuSummaryDTO;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;

// 응답 URL은 대표 URL(linku_url)이 아니라 사용자가 저장한 savedUrl을 쓴다.
// 제목, 이미지는 사용자가 수정한 UsersLinku 값을 먼저 쓰고, 없으면 Linku 값을 쓴다.
public class LinkuConverter {
    public static LinkuRequestDTO.LinkuCreateDTO toLinkuCreateDTO(
            String linku, String memo, Long emotionId, Long situationId, String title) {
        return LinkuRequestDTO.LinkuCreateDTO.builder()
                .linku(linku)
                .memo(memo)
                .emotionId(emotionId)
                .situationId(situationId)
                .title(title)
                .build();
    }

    public static LinkuResponseDTO.LinkuResultDTO toLinkuResultDTO(
            Long userId,
            Linku linku,
            UsersLinku usersLinku,
            LinkuFolder linkuFolder,
            Category category,
            String domainName,
            String domainImageUrl,
            Boolean aiArticleExists,
            String keyword,
            String summary) {
        return LinkuResponseDTO.LinkuResultDTO.builder()
                .userId(userId)
                .userLinkuId(usersLinku != null ? usersLinku.getUserLinkuId() : null)
                .folderName(
                        linkuFolder != null && linkuFolder.getFolder() != null
                                ? linkuFolder.getFolder().getFolderName()
                                : null)
                .categoryId(category != null ? category.getCategoryId() : null)
                .linku(usersLinku.getSavedUrl())
                .memo(usersLinku.getMemo())
                .emotionId(
                        usersLinku.getEmotion() != null
                                ? usersLinku.getEmotion().getEmotionId()
                                : null)
                .situationId(
                        usersLinku.getSituation() != null
                                ? usersLinku.getSituation().getId()
                                : null)
                .isEmotionAi(usersLinku.getEmotionAi())
                .isSituationAi(usersLinku.getSituationAi())
                .domain(domainName)
                .title(usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle())
                .domainImageUrl(domainImageUrl)
                .linkuImageUrl(
                        usersLinku.getImageUrl() != null
                                ? usersLinku.getImageUrl()
                                : linku.getImgUrl())
                .aiArticleExists(aiArticleExists != null ? aiArticleExists : false)
                .createdAt(linku.getCreatedAt())
                .updatedAt(linku.getUpdatedAt())
                .keyword(keyword)
                .summary(summary)
                .build();
    }

    public static LinkuResponseDTO.LinkuResultDTO toLinkuResultDTO(
            Long userId,
            Linku linku,
            UsersLinku usersLinku,
            LinkuFolder linkuFolder,
            Category category,
            Domain domain,
            Boolean aiArticleExists) {
        return LinkuResponseDTO.LinkuResultDTO.builder()
                .userId(userId)
                .userLinkuId(usersLinku.getUserLinkuId())
                .folderName(
                        linkuFolder != null && linkuFolder.getFolder() != null
                                ? linkuFolder.getFolder().getFolderName()
                                : null)
                .categoryId(category != null ? category.getCategoryId() : null)
                .linku(usersLinku.getSavedUrl())
                .memo(usersLinku.getMemo())
                .emotionId(
                        usersLinku.getEmotion() != null
                                ? usersLinku.getEmotion().getEmotionId()
                                : null)
                .situationId(
                        usersLinku.getSituation() != null
                                ? usersLinku.getSituation().getId()
                                : null)
                .isEmotionAi(usersLinku.getEmotionAi())
                .isSituationAi(usersLinku.getSituationAi())
                .domain(domain != null ? domain.getName() : null)
                .title(usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle())
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .linkuImageUrl(
                        usersLinku.getImageUrl() != null
                                ? usersLinku.getImageUrl()
                                : linku.getImgUrl())
                .aiArticleExists(aiArticleExists != null ? aiArticleExists : false)
                .createdAt(linku.getCreatedAt())
                .updatedAt(linku.getUpdatedAt())
                .build();
    }

    // 폴더 이동은 카테고리를 바꾸지 않으므로 응답에 넣지 않는다.
    public static LinkuResponseDTO.LinkuFolderChangeResultDTO toLinkuFolderChangeResultDTO(
            UsersLinku usersLinku, LinkuFolder linkuFolder) {
        Folder folder = linkuFolder != null ? linkuFolder.getFolder() : null;
        return LinkuResponseDTO.LinkuFolderChangeResultDTO.builder()
                .userLinkuId(usersLinku.getUserLinkuId())
                .folderId(folder != null ? folder.getFolderId() : null)
                .folderName(folder != null ? folder.getFolderName() : null)
                .createdAt(usersLinku.getCreatedAt())
                .updatedAt(usersLinku.getUpdatedAt())
                .build();
    }

    public static LinkuResponseDTO.LinkuIsExistDTO toLinkuIsExistDTO(
            Long userId, UsersLinku usersLinku) {
        if (usersLinku == null) {
            return LinkuResponseDTO.LinkuIsExistDTO.builder()
                    .isExist(false)
                    .userId(userId)
                    .title(null)
                    .memo(null)
                    .emotionId(null)
                    .createdAt(null)
                    .updatedAt(null)
                    .build();
        }
        return LinkuResponseDTO.LinkuIsExistDTO.builder()
                .isExist(true)
                .userId(userId)
                .title(usersLinku.getLinku().getTitle())
                .memo(usersLinku.getMemo())
                .emotionId(
                        usersLinku.getEmotion() != null
                                ? usersLinku.getEmotion().getEmotionId()
                                : null)
                .createdAt(usersLinku.getLinku().getCreatedAt())
                .updatedAt(usersLinku.getLinku().getUpdatedAt())
                .build();
    }

    public static UsersLinku toUsersLinku(
            Users user,
            Linku linku,
            Emotion emotion,
            Situation situation,
            String savedUrl,
            String memo,
            String imageUrl,
            String title,
            boolean emotionAi,
            boolean situationAi) {
        return UsersLinku.builder()
                .user(user)
                .linku(linku)
                .savedUrl(savedUrl)
                .emotion(emotion)
                .situation(situation)
                .memo(memo)
                .imageUrl(imageUrl)
                .title(title)
                .emotionAi(emotionAi)
                .situationAi(situationAi)
                .build();
    }

    public static LinkuFolder toLinkuFolder(Folder folder, UsersLinku usersLinku) {
        return LinkuFolder.builder().folder(folder).usersLinku(usersLinku).build();
    }

    public static Linku toLinku(
            String linkuUrl,
            Category category,
            Domain domain,
            String title,
            String imgUrl,
            Emotion emotion,
            Situation situation) {
        return Linku.builder()
                .linkuUrl(linkuUrl)
                .category(category)
                .domain(domain)
                .emotion(emotion)
                .situation(situation)
                .title(title != null ? title : "")
                .imgUrl(imgUrl)
                .build();
    }

    public static LinkuResponseDTO.LinkuSimpleDTO toLinkuSimpleDTO(
            Linku linku,
            UsersLinku usersLinku,
            Domain domain,
            boolean aiArticleExists,
            LinkuFolder linkuFolder) {
        return LinkuResponseDTO.LinkuSimpleDTO.builder()
                .userLinkuId(usersLinku != null ? usersLinku.getUserLinkuId() : null)
                .categoryId(
                        linku.getCategory() != null ? linku.getCategory().getCategoryId() : null)
                .folderName(linkuFolder != null ? linkuFolder.getFolder().getFolderName() : null)
                .linku(usersLinku != null ? usersLinku.getSavedUrl() : linku.getLinkuUrl())
                .memo(usersLinku != null ? usersLinku.getMemo() : null)
                .emotionId(
                        usersLinku != null && usersLinku.getEmotion() != null
                                ? usersLinku.getEmotion().getEmotionId()
                                : null)
                .title(
                        usersLinku != null && usersLinku.getTitle() != null
                                ? usersLinku.getTitle()
                                : linku.getTitle())
                .domain(domain != null ? domain.getName() : null)
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .linkuImageUrl(
                        usersLinku != null
                                ? (usersLinku.getImageUrl() != null
                                        ? usersLinku.getImageUrl()
                                        : linku.getImgUrl())
                                : linku.getImgUrl())
                .aiArticleExists(aiArticleExists)
                .lastViewedAt(usersLinku != null ? usersLinku.getLastViewedAt() : null)
                .build();
    }

    public static LinkuResponseDTO.LinkuSimpleDTO toLinkuSimpleDTO(
            RankedUsersLinku candidate, LinkuFolder linkuFolder) {
        return LinkuResponseDTO.LinkuSimpleDTO.builder()
                .userLinkuId(candidate.userLinkuId())
                .categoryId(candidate.categoryId())
                .folderName(linkuFolder != null ? linkuFolder.getFolder().getFolderName() : null)
                .linku(candidate.linku())
                .memo(candidate.memo())
                .emotionId(candidate.emotionId())
                .title(candidate.title())
                .domain(candidate.domain())
                .domainImageUrl(candidate.domainImageUrl())
                .linkuImageUrl(candidate.linkuImageUrl())
                .aiArticleExists(Boolean.TRUE.equals(candidate.aiArticleExists()))
                .lastViewedAt(candidate.lastViewedAt())
                .build();
    }

    public static LinkuResponseDTO.AiArticleSummaryDTO toAiArticleSummaryDTO(
            UsersLinku usersLinku) {
        Linku linku = usersLinku.getLinku();
        Domain domain = linku.getDomain();
        Category category = linku.getCategory();
        return LinkuResponseDTO.AiArticleSummaryDTO.builder()
                .userLinkuId(usersLinku.getUserLinkuId())
                .linku(usersLinku.getSavedUrl())
                .emotionId(
                        usersLinku.getEmotion() != null
                                ? usersLinku.getEmotion().getEmotionId()
                                : null)
                .domain(domain != null ? domain.getName() : null)
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .title(usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle())
                .linkuImageUrl(
                        usersLinku.getImageUrl() != null
                                ? usersLinku.getImageUrl()
                                : linku.getImgUrl())
                .categoryId(category != null ? category.getCategoryId() : null)
                .categoryName(category != null ? category.getCategoryName() : null)
                .build();
    }

    public static LinkuSummaryDTO toFolderLinkuSummaryDTO(UsersLinku usersLinku, String keyword) {
        Linku linku = usersLinku.getLinku();
        Domain domain = linku.getDomain();
        Category category = linku.getCategory();

        return LinkuSummaryDTO.builder()
                .userLinkuId(usersLinku.getUserLinkuId())
                .linkuId(linku.getLinkuId())
                .title(usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle())
                .url(usersLinku.getSavedUrl())
                .keyword(keyword.isEmpty() ? null : keyword)
                .linkuImageUrl(
                        usersLinku.getImageUrl() != null
                                ? usersLinku.getImageUrl()
                                : linku.getImgUrl())
                .emotionId(
                        usersLinku.getEmotion() != null
                                ? usersLinku.getEmotion().getEmotionId()
                                : null)
                .situationId(
                        usersLinku.getSituation() != null
                                ? usersLinku.getSituation().getId()
                                : null)
                .categoryId(category != null ? category.getCategoryId() : null)
                .createdAt(linku.getCreatedAt().toString())
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .domainName(domain != null ? domain.getName() : null)
                .build();
    }
}
