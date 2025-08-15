package com.umc.linkyou.repository.linkuRepository;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;

import java.util.List;
import java.util.Optional;

public interface LinkuRepositoryCustom {
    List<LinkuSearchSuggestionResponse> findUserSavedSuggestions(Long userId, String keyword);
    Optional<Linku> findByLinku(String normalizedLink);
}
