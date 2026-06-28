package com.umc.linkyou.service.keyword;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.repository.keywordRepository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 키워드 이름으로 조회하고 없으면 생성, 중복이면 기존 레코드를 반환하는 find-or-create 서비스.
 * 동시 요청 시 DataIntegrityViolationException이 발생해도 외부 트랜잭션에 영향을 주지 않도록
 * REQUIRES_NEW로 독립 트랜잭션에서 실행됩니다.
 */
@Service
@RequiredArgsConstructor
public class KeywordUpsertService {

    private final KeywordRepository keywordRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Keyword upsert(String name) {
        try {
            return keywordRepository.findByName(name)
                    .orElseGet(() -> keywordRepository.save(
                            Keyword.builder().name(name).build()
                    ));
        } catch (DataIntegrityViolationException e) {
            return keywordRepository.findByName(name)
                    .orElseThrow(() -> new GeneralException(LinkuErrorStatus._KEYWORD_NOT_FOUND));
        }
    }
}
