package com.umc.linkyou.converter;

import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import com.umc.linkyou.web.dto.AiArticleResponseDTO;

import java.util.stream.Collectors;

public class AiArticleConverter {

    public static AiArticle toEntity(AiArticleResultDTO result, Linku linku) {
        return AiArticle.builder()
                .linku(linku)
                .summary(result.summary())
                .build();
    }

    public static AiArticleResponseDTO.AiArticleResultDTO toDto(
            AiArticle entity,
            Linku linku,
            UsersLinku usersLinku
    ) {
        Emotion emotion = usersLinku != null ? usersLinku.getEmotion() : null;
        // AI 요약 호출과 별개로, 링크 저장 시 이미 분류되어 저장된 키워드를 그대로 태그로 사용한다
        // (요약할 때마다 태그를 다시 생성하지 않음 - linku 단위로 한 번 분류된 키워드는 항상 동일해야 함).
        String tags = linku.getLinkuKeywords().stream()
                .map(lk -> lk.getKeyword().getName())
                .collect(Collectors.joining(", "));
        String title = (usersLinku != null && usersLinku.getTitle() != null)
                ? usersLinku.getTitle()
                : linku.getTitle();
        return new AiArticleResponseDTO.AiArticleResultDTO(
                entity.getId(),
                linku.getLinkuId(),
                emotion != null ? emotion.getEmotionId() : null,
                emotion != null ? emotion.getName() : null,
                linku.getCategory() != null ? linku.getCategory().getCategoryName() : null,
                entity.getSummary(),
                linku.getImgUrl(),
                usersLinku != null ? usersLinku.getMemo() : null,
                tags,
                title
        );
    }
}
