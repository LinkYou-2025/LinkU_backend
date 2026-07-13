package com.umc.linkyou.repository.linkuRepository;

import com.umc.linkyou.domain.Linku;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;

import java.util.List;
import java.util.Optional;

public interface LinkuRepositoryCustom {
    List<LinkuSearchResponseDTO.LinkuSearchItemDTO> searchUserLinks(Long userId, String keyword, Long cursor, int size);
    List<LinkuQuickSearchResponseDTO> findQuickByKeyword(Long userId, String keyword);
    Optional<Linku> findByLinku(String normalizedLink);
}
