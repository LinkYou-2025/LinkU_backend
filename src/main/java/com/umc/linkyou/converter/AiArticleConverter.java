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
                .summary(result.summary())
                .build();
    }

    // tags/title은 지연 로딩 컬렉션(linku.getLinkuKeywords())과 usersLinku 우선순위 결정이 필요해
    // 트랜잭션을 쥔 서비스 레이어에서 계산해 넘겨받는다 (LinkuService/LinkuCreateService/FolderServiceImpl과
    // 동일한 패턴). 컨버터는 순수 조립만 담당한다.
    public static AiArticleResponseDTO.AiArticleResultDTO toDto(
            AiArticle entity,
            Linku linku,
            UsersLinku usersLinku,
            String tags,
            String title
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
                usersLinku != null ? usersLinku.getMemo() : null,
                tags,
                title
        );
    }
}
