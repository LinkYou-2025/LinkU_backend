package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.service.Linku.LinkuSearchService;
import com.umc.linkyou.service.Linku.LinkuService;
import com.umc.linkyou.web.dto.QuickSearchDto;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/linku")
@RequiredArgsConstructor
public class LinkuController {

    private final LinkuService linkuService;
    private final LinkuSearchService linkuSearchService;

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> createLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String linku,
            @RequestParam(required = false) String memo,
            @RequestParam(required = false) Long emotionId,
            @RequestParam(required = false) MultipartFile image
    ) {
        if (userDetails == null) {
            return ApiResponse.onFailure(
                    ErrorStatus._INVALID_TOKEN.getCode(),
                    ErrorStatus._INVALID_TOKEN.getMessage(),
                    null
            );
        }
        LinkuRequestDTO.LinkuCreateDTO linkuCreateDTO =
                LinkuConverter.toLinkuCreateDTO(linku, memo, emotionId);

        Long userId = userDetails.getUsers().getId();
        LinkuResponseDTO.LinkuResultDTO result = linkuService.createLinku(userId, linkuCreateDTO, image);
        return ApiResponse.onSuccess(result);
    }//linku 생성

    @GetMapping("/exist")
    public ResponseEntity<ApiResponse<LinkuResponseDTO.LinkuIsExistDTO>> existLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String url
    ){
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.onFailure(ErrorStatus._INVALID_TOKEN.getCode(),ErrorStatus._INVALID_TOKEN.getMessage(), null));
        }
        Long userId = userDetails.getUsers().getId();
        return linkuService.existLinku(userId, url);
    } //linku 존재여부 확인

    @GetMapping("/{linkuid}")
    public ResponseEntity<ApiResponse<LinkuResponseDTO.LinkuResultDTO>> detailLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("linkuid") Long linkuid
    ){
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.onFailure(ErrorStatus._INVALID_TOKEN.getCode(),ErrorStatus._INVALID_TOKEN.getMessage(), null));
        }
        Long userId = userDetails.getUsers().getId();
        return linkuService.detailGetLinku(userId, linkuid);
    } //linku 상세보기

    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>>> getRecentViewedLinkus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.onFailure(ErrorStatus._INVALID_TOKEN.getCode(), ErrorStatus._INVALID_TOKEN.getMessage(), null));
        }
        Long userId = userDetails.getUsers().getId();
        List<LinkuResponseDTO.LinkuSimpleDTO> result = linkuService.getRecentViewedLinkus(userId, limit);
        return ResponseEntity.ok(ApiResponse.onSuccess("최근 열람한 링크를 가져왔습니다.",result));
    } //최근 열람한 링크 보기

    @PatchMapping(value = "/{linkuId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<LinkuResponseDTO.LinkuResultDTO>> updateLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long linkuId,
            @RequestBody LinkuRequestDTO.LinkuUpdateDTO updateDTO
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.onFailure(ErrorStatus._INVALID_TOKEN.getCode(), ErrorStatus._INVALID_TOKEN.getMessage(), null));
        }
        Long userId = userDetails.getUsers().getId();
        LinkuResponseDTO.LinkuResultDTO result = linkuService.updateLinku(userId, linkuId, updateDTO);
        return ResponseEntity.ok(ApiResponse.onSuccess("링크 수정에 성공했습니다.",result));
    } //링큐 수정하기

    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>>> recommendLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long situationId,
            @RequestParam Long emotionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.onFailure(ErrorStatus._INVALID_TOKEN.getCode(),
                            ErrorStatus._INVALID_TOKEN.getMessage(),
                            null));
        }
        Long userId = userDetails.getUsers().getId();
        return linkuService.recommendLinku(userId, situationId, emotionId, page, size);
    }//linku 추천 내부로

    // 빠른 검색 (사용자가 저장한 링크 전체 대상)
    @Operation(
            summary = "빠른 검색 (사용자 저장 링크 전체 대상)",
            description = "사용자가 저장한 링크 전체를 대상으로 키워드가 포함된 추천 검색어 목록을 조회합니다."
    )
    @GetMapping("/search/quick")
    public ApiResponse<List<LinkuSearchSuggestionResponse>> quickSearch(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String keyword
    ) {
        if (userDetails == null) {
            return ApiResponse.onFailure(
                    ErrorStatus._INVALID_TOKEN.getCode(),
                    ErrorStatus._INVALID_TOKEN.getMessage(),
                    null
            );
        }

        Long userId = userDetails.getUsers().getId();
        List<LinkuSearchSuggestionResponse> result = linkuSearchService.suggest(userId, keyword);
        return ApiResponse.onSuccess(result);
    }

}
