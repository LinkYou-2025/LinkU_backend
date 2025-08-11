package com.umc.linkyou.service.Linku;

import com.umc.linkyou.repository.linkuRepository.LinkuRepositoryCustom;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkuSearchServiceImpl implements LinkuSearchService {

    private final LinkuRepositoryCustom linkuRepositoryCustom;

    @Override
    public List<LinkuSearchSuggestionResponse> suggest(Long userId, String keyword) {
        String q = (keyword == null) ? "" : keyword.trim();
        if (q.length() < 2) return List.of(); // 입력 2자 미만 가드
        return linkuRepositoryCustom.findUserSavedSuggestions(userId, q);
    }
}