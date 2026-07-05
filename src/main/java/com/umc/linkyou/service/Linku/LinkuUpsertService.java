package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LinkuUpsertService {

    private final LinkuRepository linkuRepository;

    @Transactional
    public Linku upsert(String normalizedLink, Category category, Domain domain,
                        String crawledTitle, String crawledImgUrl, Emotion aiEmotion, Situation aiSituation) {

        linkuRepository.insertIgnore(
                normalizedLink,
                crawledTitle,
                crawledImgUrl,
                category.getCategoryId(),
                domain.getDomainId(),
                aiEmotion.getEmotionId(),
                aiSituation.getId()
        );

        return linkuRepository.findByLinku(normalizedLink)
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._LINKU_NOT_FOUND));
    }
}
