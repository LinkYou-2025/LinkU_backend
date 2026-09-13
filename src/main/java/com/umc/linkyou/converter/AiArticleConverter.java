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

    // tags는 지연 로딩 컬렉션(linku.getLinkuKeywords())을 순회해야 해서 트랜잭션을 쥔 서비스
    // 레이어에서 계산해 넘겨받는다 (LinkuService/LinkuCreateService/FolderServiceImpl과 동일한 패턴).
    // title/imgUrl은 단순 필드 우선순위 결정(usersLinku 우선, 없으면 linku)이라 지연 로딩과 무관하므로
    // LinkuConverter와 동일하게 컨버터가 직접 계산한다.
    public static AiArticleResponseDTO.AiArticleResultDTO toDto(
            AiArticle entity,
            Linku linku,
            UsersLinku usersLinku,
            String tags
    ) {
        Emotion emotion = usersLinku != null ? usersLinku.getEmotion() : null;
        String title = (usersLinku != null && usersLinku.getTitle() != null)
                ? usersLinku.getTitle()
                : linku.getTitle();
        String imgUrl = (usersLinku != null && usersLinku.getImageUrl() != null)
                ? usersLinku.getImageUrl()
                : linku.getImgUrl();
        return AiArticleResponseDTO.AiArticleResultDTO.builder()
                .id(entity.getId())
                .userLinkuId(usersLinku.getUserLinkuId())
                .emotionId(emotion != null ? emotion.getEmotionId() : null)
                .emotionName(emotion != null ? emotion.getName() : null)
                .categoryName(linku.getCategory() != null ? linku.getCategory().getCategoryName() : null)
                .summary(entity.getSummary())
                .imgUrl(imgUrl)
                .memo(usersLinku != null ? usersLinku.getMemo() : null)
                .tags(tags)
                .title(title)
                .build();
    }
}
