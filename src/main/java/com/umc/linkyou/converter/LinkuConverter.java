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
import com.umc.linkyou.repository.dto.RankedUsersLinku;
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
            String domainName,
            String domainImageUrl,
            Boolean aiArticleExists,
            String keyword,
            String summary
    ) {
        return LinkuResponseDTO.LinkuResultDTO.builder()
                .userId(userId)
                .userLinkuId(usersLinku != null ? usersLinku.getUserLinkuId() : null)
                .linkuId(linku.getLinkuId())
                .folderName(linkuFolder != null && linkuFolder.getFolder() != null ? linkuFolder.getFolder().getFolderName() : null)
                .categoryId(category != null ? category.getCategoryId() : null)
                .linku(linku.getLinkuUrl())
                .memo(usersLinku.getMemo())
                .emotionId(usersLinku.getEmotion() != null ? usersLinku.getEmotion().getEmotionId() : null)
                .situationId(usersLinku.getSituation() != null ? usersLinku.getSituation().getId() : null)
                .isEmotionAi(usersLinku.getEmotionAi())
                .isSituationAi(usersLinku.getSituationAi())
                .domain(domainName)
                .title(usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle())
                .domainImageUrl(domainImageUrl)
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
                .folderName(linkuFolder != null && linkuFolder.getFolder() != null ? linkuFolder.getFolder().getFolderName() : null)
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


    // 링크 폴더 이동 → LinkuFolderChangeResultDTO 변환 (folderId는 실제 Folder PK, category는 직접 변경하지 않으므로 미포함)
    public static LinkuResponseDTO.LinkuFolderChangeResultDTO toLinkuFolderChangeResultDTO(
            Linku linku,
            LinkuFolder linkuFolder
    ) {
        Folder folder = linkuFolder != null ? linkuFolder.getFolder() : null;
        return LinkuResponseDTO.LinkuFolderChangeResultDTO.builder()
                .linkuId(linku.getLinkuId())
                .folderId(folder != null ? folder.getFolderId() : null)
                .folderName(folder != null ? folder.getFolderName() : null)
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
    public static LinkuResponseDTO.LinkuSimpleDTO toLinkuSimpleDTO(Linku linku, UsersLinku usersLinku, Domain domain, boolean aiArticleExists, LinkuFolder linkuFolder) {
        return LinkuResponseDTO.LinkuSimpleDTO.builder()
                .userLinkuId(usersLinku != null ? usersLinku.getUserLinkuId() : null)
                .linkuId(linku.getLinkuId())
                .categoryId(linku.getCategory() != null ? linku.getCategory().getCategoryId() : null)
                .folderName(linkuFolder != null ? linkuFolder.getFolder().getFolderName() : null)
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

    public static LinkuResponseDTO.LinkuSimpleDTO toLinkuSimpleDTO(
            RankedUsersLinku candidate, LinkuFolder linkuFolder) {
        return LinkuResponseDTO.LinkuSimpleDTO.builder()
                .userLinkuId(candidate.userLinkuId())
                .linkuId(candidate.linkuId())
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

    // 마이페이지 AI 요약 링크 목록용 - toLinkuSimpleDTO와 동일한 title/linkuImageUrl 우선순위 패턴
    // (usersLinku 우선, 없으면 linku)을 사용한다.
    public static LinkuResponseDTO.AiArticleSummaryDTO toAiArticleSummaryDTO(UsersLinku usersLinku) {
        Linku linku = usersLinku.getLinku();
        Domain domain = linku.getDomain();
        Category category = linku.getCategory();
        return LinkuResponseDTO.AiArticleSummaryDTO.builder()
                .linkuId(linku.getLinkuId())
                .linku(linku.getLinkuUrl())
                .emotionId(usersLinku.getEmotion() != null ? usersLinku.getEmotion().getEmotionId() : null)
                .domain(domain != null ? domain.getName() : null)
                .domainImageUrl(domain != null ? domain.getImageUrl() : null)
                .title(usersLinku.getTitle() != null ? usersLinku.getTitle() : linku.getTitle())
                .linkuImageUrl(usersLinku.getImageUrl() != null ? usersLinku.getImageUrl() : linku.getImgUrl())
                .categoryId(category != null ? category.getCategoryId() : null)
                .categoryName(category != null ? category.getCategoryName() : null)
                .build();
    }

}
