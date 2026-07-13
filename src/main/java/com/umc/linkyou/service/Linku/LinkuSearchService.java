package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LinkuSearchService{

    private final LinkuRepository linkuRepository;

    // 링크 검색 (커서 페이징)
    public LinkuSearchResponseDTO.LinkuSearchCursorPageResponse search(Long userId, String keyword, Long cursor, int size) {
        String trimmed = validateKeyword(keyword);

        if (size <= 0) {
            return new LinkuSearchResponseDTO.LinkuSearchCursorPageResponse(List.of(), null, false);
        }

        // size+1개 조회해서 다음 페이지 존재 여부 판단
        List<LinkuSearchResponseDTO.LinkuSearchItemDTO> fetched =
                linkuRepository.searchUserLinks(userId, trimmed, cursor, size);

        boolean hasNext = fetched.size() > size;
        List<LinkuSearchResponseDTO.LinkuSearchItemDTO> items = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = items.isEmpty() ? null : items.get(items.size() - 1).userLinkuId();

        return new LinkuSearchResponseDTO.LinkuSearchCursorPageResponse(items, nextCursor, hasNext);
    }

    // 검색어 자동완성 (퀵서치)
    public List<LinkuQuickSearchResponseDTO> quickSearch(Long userId, String keyword) {
        return linkuRepository.findQuickByKeyword(userId, validateKeyword(keyword));
    }

    private String validateKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new GeneralException(LinkuErrorStatus._LINKU_SEARCH_KEYWORD_REQUIRED);
        }
        String trimmed = keyword.trim();
        if (trimmed.length() < 2) {
            throw new GeneralException(LinkuErrorStatus._LINKU_SEARCH_KEYWORD_TOO_SHORT);
        }
        return trimmed;
    }
}
