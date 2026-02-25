package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.DomainConverter;
import com.umc.linkyou.service.domain.DomainService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.DomainDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "domain-controller", description = "도메인 관련 API")
@ApiV1
@RestController
@RequestMapping("/domain")
@RequiredArgsConstructor
public class DomainController {

    private final DomainService domainService;
    private final UsersUtils usersUtils;

    @Operation(
            summary = "도메인 생성",
            description = "새로운 도메인을 생성합니다. 도메인 이름, 도메인 꼬리, 이미지를 포함할 수 있습니다."
    )
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

    @Operation(
            summary = "도메인 수정",
            description = "기존 도메인의 정보를 수정합니다. 도메인 이름, 도메인 꼬리, 이미지를 수정할 수 있습니다."
    )
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
