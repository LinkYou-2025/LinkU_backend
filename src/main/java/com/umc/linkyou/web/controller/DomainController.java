package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.converter.DomainConverter;
import com.umc.linkyou.service.domain.DomainService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.DomainApi;
import com.umc.linkyou.web.dto.DomainDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@ApiV1
@RequiredArgsConstructor
public class DomainController implements DomainApi {

    private final DomainService domainService;

    @Override
    public ApiResponse<DomainDTO.DomainReponseDTO> createDomain(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam(required = false) String name, @RequestParam(required = false) String domainTail, @RequestParam(required = false) MultipartFile image) {
        DomainDTO.DomainRequestDTO domainCreateDTO = DomainConverter.toDomainCreateDTO(name, domainTail);
        return ApiResponse.onSuccess(domainService.createDomain(userDetails.getUserId(), domainCreateDTO, image));
    }

    @Override
    public ApiResponse<DomainDTO.DomainReponseDTO> updateDomain(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestParam Long id, @RequestParam(required = false) String name, @RequestParam(required = false) String domainTail, @RequestParam(required = false) MultipartFile image) {
        DomainDTO.DomainRequestDTO domainUpdateDTO = DomainDTO.DomainRequestDTO.builder()
                .id(id)
                .name(name)
                .domainTail(domainTail)
                .build();
        return ApiResponse.onSuccess(domainService.updateDomain(userDetails.getUserId(), domainUpdateDTO, image));
    }
}
