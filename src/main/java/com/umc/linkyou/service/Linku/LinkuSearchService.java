package com.umc.linkyou.service.Linku;

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
        String q = (keyword == null) ? "" : keyword.trim();
        return linkuRepository.findUserSavedSuggestions(userId, q);
    }
}
