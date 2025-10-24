package com.umc.linkyou.converter;

import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.openApiSummary.SummaryAnalysisResultDTO;
import com.umc.linkyou.web.dto.AiArticleResponsetDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class AiArticleConverter {
    // 엔티티 생성 메서드
    public static AiArticle toEntity(SummaryAnalysisResultDTO result,
                                     Situation selectedSituation,
                                     Emotion selectedEmotion,
                                     Category selectedCategory,
                                     Linku linku,
                                     String imageUrl)
    {
        return AiArticle.builder()
                .linku(linku)
                .situation(selectedSituation)
                .title(result.getTitle())
                .aiFeelingId(selectedEmotion.getEmotionId())
                .aiCategoryId(selectedCategory.getCategoryId())
                .summary(result.getSummary())
                .imgUrl(imageUrl)
                .build();
    }

    // DTO 생성 메서드
    public static AiArticleResponsetDTO.AiArticleResultDTO toDto(
            AiArticle entity,
            Linku linku,
            UsersLinku usersLinku,
            Situation selectedSituation,
            Emotion selectedEmotion,
            Category selectedCategory
    ) {
        return AiArticleResponsetDTO.AiArticleResultDTO.builder()
                .id(entity.getId())
                .linkuId(linku.getLinkuId())
                .situationId(selectedSituation != null ? selectedSituation.getId() : null)
                .situationName(selectedSituation != null ? selectedSituation.getName() : null)
                .emotionId(selectedEmotion != null ? selectedEmotion.getEmotionId() : null)
                .emotionName(selectedEmotion != null ? selectedEmotion.getName() : null)
                .title(entity.getTitle())
                .aiFeelingName(selectedEmotion != null ? selectedEmotion.getName() : null)
                .aiFeelingId(entity.getAiFeelingId())
                .aiCategoryId(entity.getAiCategoryId())
                .categoryName(selectedCategory != null ? selectedCategory.getCategoryName() : null)
                .summary(entity.getSummary())
                .imgUrl(entity.getImgUrl())
                .memo(usersLinku != null ? usersLinku.getMemo() : null)
                .keyword(entity.getKeyword())
                .build();
    }
    public static AiArticle toEntityKeywordOnly(
            String keyword,
            Linku linku,
            Situation defaultSituation,
            Category defaultCategory,
            Emotion defaultEmotion
    ) {
        return AiArticle.builder()
                .linku(linku)
                .situation(defaultSituation)           // nullable=false
                .title("")                             // nullable=false
                .aiFeelingId(defaultEmotion.getEmotionId())
                .aiCategoryId(defaultCategory.getCategoryId())
                .summary("")                           // nullable=false
                .imgUrl(null)
                .keyword(keyword)                      // 실제로 넣고 싶은 값
                .build();
    }
}
