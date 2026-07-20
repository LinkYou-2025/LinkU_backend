package com.umc.linkyou.service.keyword;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Keyword;
import com.umc.linkyou.repository.keywordRepository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KeywordUpsertService {

    private final KeywordRepository keywordRepository;

    @Transactional
    public Keyword upsert(String name) {
        keywordRepository.insertIgnore(name);

        return keywordRepository.findByName(name)
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._KEYWORD_NOT_FOUND));
    }
}
