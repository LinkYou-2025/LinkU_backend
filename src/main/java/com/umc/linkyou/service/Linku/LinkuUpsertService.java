package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * URL(링크)을 DB에 저장하되, 중복이면 기존 레코드를 반환하는 insert-or-select 서비스.
 * 동시 요청 시 DataIntegrityViolationException이 발생해도 외부 트랜잭션에 영향을 주지 않도록
 * REQUIRES_NEW로 독립 트랜잭션에서 실행됩니다.
 */
@Service
@RequiredArgsConstructor
public class LinkuUpsertService {

    private final LinkuRepository linkuRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Linku upsert(String normalizedLink, Category category, Domain domain,
                        String crawledTitle, String crawledImgUrl, Emotion aiEmotion, Situation aiSituation) {
        try {
            return linkuRepository.save(
                    LinkuConverter.toLinku(normalizedLink, category, domain, crawledTitle, crawledImgUrl, aiEmotion, aiSituation)
            );
        } catch (DataIntegrityViolationException e) {
            return linkuRepository.findByLinku(normalizedLink)
                    .orElseThrow(() -> new GeneralException(LinkuErrorStatus._LINKU_NOT_FOUND));
        }
    }
}
