package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.DomainDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "domain-controller", description = "도메인 관련 API")
@RequestMapping("/domain")
public interface DomainApi {

    @Operation(summary = "도메인 생성", description = """
    새로운 도메인을 생성합니다. 도메인 이름, 도메인 꼬리, 이미지를 포함할 수 있습니다.
    Content-Type 주의: 이 API는 파일 업로드(image)를 지원하기 위해 multipart/form-data로만 요청을 받습니다. Swagger UI에는 (MultipartFile을 제외한) 나머지 파라미터들이 편의상 query 타입으로 표시되지만, 이는 문서화 라이브러리(springdoc)의 표기 방식일 뿐이며 실제로는 반드시 multipart/form-data 요청의 폼 필드로 함께 전송해야 합니다. 순수 쿼리스트링이나 application/json으로 보내면 415(Unsupported Media Type)가 발생합니다.
    """)
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<DomainDTO.DomainReponseDTO> createDomain(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String domainTail,
            @RequestParam(required = false) MultipartFile image
    );

    @Operation(summary = "도메인 수정", description = """
    기존 도메인의 정보를 수정합니다. 도메인 이름, 도메인 꼬리, 이미지를 수정할 수 있습니다.
    Content-Type 주의: 이 API는 파일 업로드(image)를 지원하기 위해 multipart/form-data로만 요청을 받습니다. Swagger UI에는 (MultipartFile을 제외한) 나머지 파라미터들이 편의상 query 타입으로 표시되지만, 이는 문서화 라이브러리(springdoc)의 표기 방식일 뿐이며 실제로는 반드시 multipart/form-data 요청의 폼 필드로 함께 전송해야 합니다. 순수 쿼리스트링이나 application/json으로 보내면 415(Unsupported Media Type)가 발생합니다.    
    """)
    @ApiErrorCode(errorStatus = {ErrorStatus._DOMAIN_NOT_FOUND})
    @PatchMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<DomainDTO.DomainReponseDTO> updateDomain(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String domainTail,
            @RequestParam(required = false) MultipartFile image
    );
}
