package com.umc.linkyou.converter;

import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.folder.Folder;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.mapping.LinkuFolder;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;

public class LinkuConverter {
    // Converter: RequestParam으로 받은 데이터 -> LinkuCreateDTO 생성
    public static LinkuRequestDTO.LinkuCreateDTO toLinkuCreateDTO(String linku, String memo, Long emotionId) {
        return LinkuRequestDTO.LinkuCreateDTO.builder()
                .linku(linku)
                .memo(memo)
                .emotionId(emotionId)
                .build();
    }

    // Linku생성 → LinkuResultDTO 변환
    public static LinkuResponseDTO.LinkuResultDTO toLinkuResultDTO(
            Long userId,
            Linku linku,
            UsersLinku usersLinku,
            LinkuFolder linkuFolder,
            Category category,
            Domain domain
    ) {
        return LinkuResponseDTO.LinkuResultDTO.builder()
                .userId(userId)
                .linkuId(linku.getLinkuId())
                .linkuFolderId(linkuFolder.getLinkuFolderId())
                .categoryId(category.getCategoryId())
                .linku(linku.getLinku())
                .memo(usersLinku.getMemo())
                .emotionId(usersLinku.getEmotion().getEmotionId())
                .domain(domain.getName())
                .title(linku.getTitle())
                .domainImageUrl(domain.getImageUrl())
                .linkuImageUrl(usersLinku.getImageUrl())
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
                .emotionId(usersLinku.getEmotion().getEmotionId())
                .createdAt(usersLinku.getLinku().getCreatedAt())
                .updatedAt(usersLinku.getLinku().getUpdatedAt())
                .build();
    }
    // UsersLinku 생성
    public static UsersLinku toUsersLinku(Users user, Linku linku, Emotion emotion, String memo, String imageUrl) {
        return UsersLinku.builder()
                .user(user)
                .linku(linku)
                .emotion(emotion)
                .memo(memo)
                .imageUrl(imageUrl)
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
    public static Linku toLinku(String linkuUrl, Category category, Domain domain, String title) {
        return Linku.builder()
                .linku(linkuUrl)
                .category(category)
                .domain(domain)
                .title(title != null ? title : "")
                .build();
    }
    public static LinkuResponseDTO.LinkuSimpleDTO toLinkuSimpleDTO(Linku linku, UsersLinku usersLinku, Domain domain, boolean aiArticleExists) {
        return LinkuResponseDTO.LinkuSimpleDTO.builder()
                .linkuId(linku.getLinkuId())
                .categoryId(linku.getCategory() != null ? linku.getCategory().getCategoryId() : null)
                .memo(usersLinku != null ? usersLinku.getMemo() : null)
                .emotionId(usersLinku != null && usersLinku.getEmotion() != null ? usersLinku.getEmotion().getEmotionId() : null)
                .title(linku.getTitle())
                .domain(domain != null ? domain.getName() : null)
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .linkuImageUrl(usersLinku != null ? usersLinku.getImageUrl() : null)
                .aiArticleExists(aiArticleExists)
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
                .title(linku.getTitle())
                .domain(domain != null ? domain.getName() : null)
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .linkuImageUrl(usersLinku.getImageUrl())
                .build();
    }

    public static RecommendedLinkResponse toRecommendedLinkResponse(UsersLinku usersLinku) {
        return RecommendedLinkResponse.builder()
                .userLinkuId(usersLinku.getUserLinkuId())
                .title(usersLinku.getLinku().getTitle())
                .url(usersLinku.getLinku().getLinku())
                .domain(usersLinku.getLinku().getDomain().getName())
                .imageUrl(usersLinku.getImageUrl())
                .build();
    }

}
