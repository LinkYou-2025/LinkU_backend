package com.umc.linkyou.service.Linku;

import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.LinkuSearchHistory;
import com.umc.linkyou.repository.LinkuSearchHistoryRepository;
import com.umc.linkyou.repository.linkuRepository.LinkuRepository;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchHistoryItemDTO;
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
    private final LinkuSearchHistoryRepository linkuSearchHistoryRepository;

    // 링크 검색 (커서 페이징)
    @Transactional
    public LinkuSearchResponseDTO.LinkuSearchCursorPageResponse search(Long userId, String searchQuery, Long cursor, int size) {
        String trimmed = validateSearchQuery(searchQuery);

        if (size <= 0) {
            return new LinkuSearchResponseDTO.LinkuSearchCursorPageResponse(List.of(), null, false);
        }

        // size+1개 조회해서 다음 페이지 존재 여부 판단
        List<LinkuSearchResponseDTO.LinkuSearchItemDTO> fetched =
                linkuRepository.searchUserLinks(userId, trimmed, cursor, size);

        boolean hasNext = fetched.size() > size;
        List<LinkuSearchResponseDTO.LinkuSearchItemDTO> items = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = hasNext ? items.get(items.size() - 1).userLinkuId() : null;

        // 동일 키워드 기존 기록 제거 후 저장 (중복 제거, 최신순 재정렬)
        linkuSearchHistoryRepository.deleteByUserIdAndKeyword(userId, trimmed);
        linkuSearchHistoryRepository.save(LinkuSearchHistory.of(userId, trimmed));
        // 10개 넘으면 가장 오래된 것 삭제
        if (linkuSearchHistoryRepository.countByUserId(userId) > 10) {
            linkuSearchHistoryRepository.findFirstByUserIdOrderByCreatedAtAsc(userId)
                    .ifPresent(h -> linkuSearchHistoryRepository.deleteById(h.getId()));
        }

        return new LinkuSearchResponseDTO.LinkuSearchCursorPageResponse(items, nextCursor, hasNext);
    }

    // 검색어 모두 삭제 (기록 없어도 정상 처리)
    @Transactional
    public void deleteAllKeywords(Long userId) {
        linkuSearchHistoryRepository.deleteAllByUserId(userId);
    }

    // 검색어 단일 삭제
    @Transactional
    public void deleteKeyword(Long userId, Long searchHistoryId) {
        // 키워드 조회
        LinkuSearchHistory history = linkuSearchHistoryRepository.findByUserIdAndId(userId, searchHistoryId)
                .orElseThrow(() -> new GeneralException(LinkuErrorStatus._SEARCH_HISTORY_NOT_FOUND));
        linkuSearchHistoryRepository.delete(history);
    }

    // 최근 검색어 조회
    public List<LinkuSearchHistoryItemDTO> getRecentKeywords(Long userId) {
        return linkuSearchHistoryRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(h -> new LinkuSearchHistoryItemDTO(h.getId(), h.getKeyword()))
                .toList();
    }
    
    // 검색어 자동완성
    public List<LinkuQuickSearchResponseDTO> quickSearch(Long userId, String searchQuery) {
        return linkuRepository.findQuickByKeyword(userId, validateSearchQuery(searchQuery));
    }

    private String validateSearchQuery(String searchQuery) {
        if (searchQuery == null || searchQuery.trim().isEmpty()) {
            throw new GeneralException(LinkuErrorStatus._LINKU_SEARCH_KEYWORD_REQUIRED);
        }
        String trimmed = searchQuery.trim();
        if (trimmed.length() < 2) {
            throw new GeneralException(LinkuErrorStatus._LINKU_SEARCH_KEYWORD_TOO_SHORT);
        }
        return trimmed;
    }
}
