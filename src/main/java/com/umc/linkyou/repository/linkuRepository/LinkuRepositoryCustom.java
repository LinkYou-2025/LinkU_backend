package com.umc.linkyou.repository.linkuRepository;

import java.util.List;
import java.util.Optional;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.web.dto.keyword.KeywordLinkuItemDTO;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;

public interface LinkuRepositoryCustom {
    List<LinkuSearchResponseDTO.LinkuSearchItemDTO> searchUserLinks(
            Long userId, String keyword, Long cursor, int size);

    List<LinkuQuickSearchResponseDTO> findQuickByKeyword(Long userId, String keyword);

    Optional<Linku> findByLinku(String normalizedLink);

    List<KeywordLinkuItemDTO> findUserLinksByExactKeyword(Long userId, String keyword);
}
