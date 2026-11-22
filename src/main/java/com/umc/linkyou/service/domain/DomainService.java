package com.umc.linkyou.service.domain;

import org.springframework.web.multipart.MultipartFile;

import com.umc.linkyou.web.dto.DomainDTO;

public interface DomainService {
    DomainDTO.DomainReponseDTO createDomain(
            Long userId, DomainDTO.DomainRequestDTO dto, MultipartFile image);

    DomainDTO.DomainReponseDTO updateDomain(
            Long userId, DomainDTO.DomainRequestDTO domainCreateDTO, MultipartFile image);

    DomainDTO.DomainCursorPageResponse getDomainsCursor(Long lastDomainId, int size);
}
