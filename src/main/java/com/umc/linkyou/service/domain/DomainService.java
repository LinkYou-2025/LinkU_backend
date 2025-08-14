package com.umc.linkyou.service.domain;

import com.umc.linkyou.web.dto.DomainDTO;
import org.springframework.web.multipart.MultipartFile;

public interface DomainService {
    DomainDTO.DomainReponseDTO createDomain(Long userId, DomainDTO.DomainRequestDTO dto, MultipartFile image);
    DomainDTO.DomainReponseDTO updateDomain(Long userId, DomainDTO.DomainRequestDTO domainCreateDTO, MultipartFile image);
    DomainDTO.DomainCursorPageResponse getDomainsCursor(Long lastDomainId, int size);
}
