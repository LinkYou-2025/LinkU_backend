package com.umc.linkyou.converter;

import com.umc.linkyou.web.dto.DomainDTO;

public class DomainConverter {

    public static DomainDTO.DomainRequestDTO toDomainCreateDTO(String name, String domainTail) {
        return DomainDTO.DomainRequestDTO.builder().name(name).domainTail(domainTail).build();
    }

    public static DomainDTO.DomainReponseDTO toDomainResponseDTO(
            String name, String domainTail, String imageUrl) {
        return DomainDTO.DomainReponseDTO.builder()
                .name(name)
                .domainTail(domainTail)
                .imageUrl(imageUrl)
                .build();
    }
}
