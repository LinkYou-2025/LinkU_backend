package com.umc.linkyou.repository.linkuRepository;

import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;

import java.util.List;

public interface LinkuRepositoryCustom {
    List<LinkuSearchSuggestionResponse> findUserSavedSuggestions(Long userId, String keyword);
}
