package com.umc.linkyou.converter;

import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;

public class LinkuConverter {
    // Converter: RequestParam으로 받은 데이터 -> LinkuCreateDTO 생성
    public static LinkuRequestDTO.LinkuCreateDTO toLinkuCreateDTO(String linku, String memo, Long emotionId, Long situationId, String title) {
        return LinkuRequestDTO.LinkuCreateDTO.builder()
                .linku(linku)
                .memo(memo)
                .emotionId(emotionId)
                .situationId(situationId)
                .title(title)
                .build();
    }

    // Linku생성 → LinkuResultDTO 변환
    public static LinkuResponseDTO.LinkuResultDTO toLinkuResultDTO(
            Long userId,
            Linku linku,
            UsersLinku usersLinku,
            LinkuFolder linkuFolder,
            Category category,
            Domain domain,
            Boolean aiArticleExists,
            String keyword,
            String summary
    ) {
        return LinkuResponseDTO.LinkuResultDTO.builder()
                .userId(userId)
                .userLinkuId(usersLinku != null ? usersLinku.getUserLinkuId() : null)
                .linkuId(linku.getLinkuId())
                .linkuFolderId(linkuFolder != null ? linkuFolder.getLinkuFolderId() : null)
                .categoryId(category != null ? category.getCategoryId() : null)
                .linku(linku.getLinkuUrl())
                .memo(usersLinku.getMemo())
                .emotionId(usersLinku.getEmotion() != null ? usersLinku.getEmotion().getEmotionId() : null)
                .situationId(usersLinku.getSituation() != null ? usersLinku.getSituation().getId() : null)
                .isEmotionAi(usersLinku.getEmotionAi())
                .isSituationAi(usersLinku.getSituationAi())
                .domain(domain != null ? domain.getName() : null)
                .title(usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle())
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .linkuImageUrl(usersLinku.getImageUrl() != null ? usersLinku.getImageUrl() : linku.getImgUrl())
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
            Boolean aiArticleExists
    ) {
        return LinkuResponseDTO.LinkuResultDTO.builder()
                .userId(userId)
                .linkuId(linku.getLinkuId())
                .linkuFolderId(linkuFolder != null ? linkuFolder.getLinkuFolderId() : null)
                .categoryId(category != null ? category.getCategoryId() : null)
                .linku(linku.getLinkuUrl())
                .memo(usersLinku.getMemo())
                .emotionId(usersLinku.getEmotion() != null ? usersLinku.getEmotion().getEmotionId() : null)
                .situationId(usersLinku.getSituation() != null ? usersLinku.getSituation().getId() : null)
                .isEmotionAi(usersLinku.getEmotionAi())
                .isSituationAi(usersLinku.getSituationAi())
                .domain(domain != null ? domain.getName() : null)
                .title(usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle())
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .linkuImageUrl(usersLinku.getImageUrl() != null ? usersLinku.getImageUrl() : linku.getImgUrl())
                .aiArticleExists(aiArticleExists != null ? aiArticleExists : false)
                .createdAt(linku.getCreatedAt())
                .updatedAt(linku.getUpdatedAt())
                .build();
    }


    // Linku -> LinkuIsExistDTO 변환
    public static LinkuResponseDTO.LinkuIsExistDTO toLinkuIsExistDTO(Long userId, UsersLinku usersLinku) {
        if (usersLinku == null) {
            return LinkuResponseDTO.LinkuIsExistDTO.builder()
                    .isExist(false)
                    .userId(userId)
                    .linkuId(null)
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
                .linkuId(usersLinku.getLinku().getLinkuId())
                .title(usersLinku.getLinku().getTitle())
                .memo(usersLinku.getMemo())
                .emotionId(usersLinku.getEmotion() != null ? usersLinku.getEmotion().getEmotionId() : null)
                .createdAt(usersLinku.getLinku().getCreatedAt())
                .updatedAt(usersLinku.getLinku().getUpdatedAt())
                .build();
    }
    // UsersLinku 생성
    public static UsersLinku toUsersLinku(Users user, Linku linku, Emotion emotion, Situation situation,
                                          String memo, String imageUrl, String title,
                                          boolean emotionAi, boolean situationAi) {
        return UsersLinku.builder()
                .user(user)
                .linku(linku)
                .emotion(emotion)
                .situation(situation)
                .memo(memo)
                .imageUrl(imageUrl)
                .title(title)
                .emotionAi(emotionAi)
                .situationAi(situationAi)
                .build();
    }

    //LinkuFolder 생성
    public static LinkuFolder toLinkuFolder(Folder folder, UsersLinku usersLinku) {
        return LinkuFolder.builder()
                .folder(folder)
                .usersLinku(usersLinku)
                .build();
    }

    // Linku 생성
    public static Linku toLinku(String linkuUrl, Category category, Domain domain, String title, String imgUrl, Emotion emotion, Situation situation) {
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
    public static LinkuResponseDTO.LinkuSimpleDTO toLinkuSimpleDTO(Linku linku, UsersLinku usersLinku, Domain domain, boolean aiArticleExists) {
        return LinkuResponseDTO.LinkuSimpleDTO.builder()
                .linkuId(linku.getLinkuId())
                .categoryId(linku.getCategory() != null ? linku.getCategory().getCategoryId() : null)
                .linku(linku.getLinkuUrl())
                .memo(usersLinku != null ? usersLinku.getMemo() : null)
                .emotionId(usersLinku != null && usersLinku.getEmotion() != null ? usersLinku.getEmotion().getEmotionId() : null)
                .title(usersLinku != null && usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle())
                .domain(domain != null ? domain.getName() : null)
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .linkuImageUrl(usersLinku != null ? (usersLinku.getImageUrl() != null ? usersLinku.getImageUrl() : linku.getImgUrl()) : linku.getImgUrl())
                .aiArticleExists(aiArticleExists)
                .lastViewedAt(usersLinku != null ? usersLinku.getLastViewedAt() : null)
                .build();
    } //리스트로 반환할때 쓰이는 것
    public static LinkuResponseDTO.LinkuSimpleDTO toLinkuSimpleDTO(UsersLinku usersLinku) {
        if (usersLinku == null) return null;

        Linku linku = usersLinku.getLinku();
        Domain domain = linku.getDomain();

        return LinkuResponseDTO.LinkuSimpleDTO.builder()
                .linkuId(linku.getLinkuId())
                .categoryId(linku.getCategory() != null ? linku.getCategory().getCategoryId() : null)
                .memo(usersLinku.getMemo())
                .emotionId(usersLinku.getEmotion() != null ? usersLinku.getEmotion().getEmotionId() : null)
                .title(usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle())
                .domain(domain != null ? domain.getName() : null)
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .linkuImageUrl(usersLinku.getImageUrl() != null ? usersLinku.getImageUrl() : linku.getImgUrl())
                .build();
    }

}
