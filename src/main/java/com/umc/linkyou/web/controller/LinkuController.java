package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.service.Linku.LinkuCreateService;
import com.umc.linkyou.service.Linku.LinkuRecommendService;
import com.umc.linkyou.service.Linku.LinkuSearchService;
import com.umc.linkyou.service.Linku.LinkuService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/linku")
@RequiredArgsConstructor
public class LinkuController {

    private final LinkuService linkuService;
    private final LinkuCreateService linkuCreateService;
    private final LinkuSearchService linkuSearchService;
    private final LinkuRecommendService linkuRecommendService;
    private final UsersUtils usersUtils;

    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> createLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String linku,
            @RequestParam(required = false) String memo,
            @RequestParam(required = false) Long emotionId,
            @RequestParam(required = false) MultipartFile image
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        LinkuRequestDTO.LinkuCreateDTO linkuCreateDTO =
                LinkuConverter.toLinkuCreateDTO(linku, memo, emotionId);

        LinkuResponseDTO.LinkuCreateResult serviceResult = linkuCreateService.createLinku(userId, linkuCreateDTO, image);

        if (serviceResult.isValidUrl()) {
            return ApiResponse.of(SuccessStatus._OK, serviceResult.getData());
        } else {
            return ApiResponse.of(SuccessStatus._LINKU_SUS_URL, serviceResult.getData());
        }
    }//linku 생성

    @GetMapping("/exist")
    public ApiResponse<LinkuResponseDTO.LinkuIsExistDTO> existLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String url
    ){
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        return linkuService.existLinku(userId, url);
    }//linku 존재여부 확인

    @GetMapping("/{linkuid}")
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable("linkuid") Long linkuid
    ){
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        return linkuService.detailGetLinku(userId, linkuid);
    } //linku 상세보기

    @GetMapping("/{userId}/{linkuId}")
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailLinku(
            @PathVariable Long userId,
            @PathVariable Long linkuId) {
        return linkuService.detailGetLinku(userId, linkuId);
    }//userId를 받아서 상세보기

    @GetMapping("/recent")
    public ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> getRecentViewedLinkus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        List<LinkuResponseDTO.LinkuSimpleDTO> result = linkuService.getRecentViewedLinkus(userId, limit);
        return ApiResponse.onSuccess("최근 열람한 링크를 가져왔습니다.",result);
    } //최근 열람한 링크 보기

    @PatchMapping(value = "/{linkuId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> updateLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long linkuId,
            @RequestBody LinkuRequestDTO.LinkuUpdateDTO updateDTO
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        LinkuResponseDTO.LinkuResultDTO result = linkuService.updateLinku(userId, linkuId, updateDTO);
        return ApiResponse.onSuccess("링크 수정에 성공했습니다.",result);
    } //링큐 수정하기

    @GetMapping("/recommend")
    public ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> recommendLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long situationId,
            @RequestParam Long emotionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        return linkuRecommendService.recommendLinku(userId, situationId, emotionId, page, size);
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
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        List<LinkuSearchSuggestionResponse> result = linkuSearchService.suggest(userId, keyword);
        return ApiResponse.onSuccess(result);
    }

    @DeleteMapping("/{userLinkuId}")
    public ApiResponse<Void> deleteUsersLinku(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userLinkuId
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        linkuService.deleteUsersLinku(userId, userLinkuId);
        return ApiResponse.<Void>onSuccess("링크 삭제에 성공했습니다.", null);

    }

}
