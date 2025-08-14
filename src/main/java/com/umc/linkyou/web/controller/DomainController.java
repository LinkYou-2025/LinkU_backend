package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.DomainConverter;
import com.umc.linkyou.service.domain.DomainService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.web.dto.DomainDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/domain")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;
    private final UsersUtils usersUtils;

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DomainDTO.DomainReponseDTO> createLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String domainTail,
            @RequestParam(required = false) MultipartFile image
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        DomainDTO.DomainRequestDTO domainCreateDTO = DomainConverter.toDomainCreateDTO(name, domainTail);

        DomainDTO.DomainReponseDTO result = domainService.createDomain(userId,domainCreateDTO, image);
        return ApiResponse.onSuccess(result);
    }

    @PatchMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DomainDTO.DomainReponseDTO> updateLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String domainTail,
            @RequestParam(required = false) MultipartFile image
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        // id 포함한 DTO 생성
        DomainDTO.DomainRequestDTO domainUpdateDTO = DomainDTO.DomainRequestDTO.builder()
                .id(id)
                .name(name)
                .domainTail(domainTail)
                .build();

        DomainDTO.DomainReponseDTO result = domainService.updateDomain(userId, domainUpdateDTO, image);
        return ApiResponse.onSuccess(result);
    }

}
