package com.umc.linkyou.converter;

import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import com.umc.linkyou.web.dto.AiArticleResponseDTO;

public class AiArticleConverter {

    public static AiArticle toEntity(AiArticleResultDTO result, Linku linku) {
        return AiArticle.builder()
                .linku(linku)
                .title(linku.getTitle())
                .summary(result.summary())
                .build();
    }

    public static AiArticleResponseDTO.AiArticleResultDTO toDto(
            AiArticle entity,
            Linku linku,
            UsersLinku usersLinku
    ) {
        Emotion emotion = usersLinku != null ? usersLinku.getEmotion() : null;
        return new AiArticleResponseDTO.AiArticleResultDTO(
                entity.getId(),
                linku.getLinkuId(),
                emotion != null ? emotion.getEmotionId() : null,
                emotion != null ? emotion.getName() : null,
                linku.getCategory() != null ? linku.getCategory().getCategoryName() : null,
                entity.getSummary(),
                linku.getImgUrl(),
                usersLinku != null ? usersLinku.getMemo() : null
        );
    }
}
