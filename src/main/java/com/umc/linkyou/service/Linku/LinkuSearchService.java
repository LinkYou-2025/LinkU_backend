package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkuSearchService{

    private final LinkuRepository linkuRepository;

    @Transactional(readOnly = true)
    public List<LinkuSearchSuggestionResponse> suggest(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new GeneralException(LinkuErrorStatus._LINKU_SEARCH_KEYWORD_REQUIRED);
        }
        return linkuRepository.findUserSavedSuggestions(userId, keyword.trim());
    }
}
