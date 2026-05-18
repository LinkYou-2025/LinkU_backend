package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuSuccessStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.service.Linku.LinkuCreateService;
import com.umc.linkyou.service.Linku.LinkuRecommendService;
import com.umc.linkyou.service.Linku.LinkuSearchService;
import com.umc.linkyou.service.Linku.LinkuService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "linku-controller", description = "링크(Linku) 관련 API")
@ApiV1
@RestController
@RequestMapping("/linku")
@RequiredArgsConstructor
public class LinkuController {

    private final LinkuService linkuService;
    private final LinkuCreateService linkuCreateService;
    private final LinkuSearchService linkuSearchService;
    private final LinkuRecommendService linkuRecommendService;

    @Operation(
            summary = "링크 생성",
            description = "새로운 링크를 생성합니다. URL, 메모, 감정 ID, 이미지를 포함할 수 있습니다."
    )
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> createLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String linku,
            @RequestParam(required = false) String memo,
            @RequestParam(required = false) Long emotionId,
            @RequestParam(required = false) MultipartFile image
    ) {
        Long userId = userDetails.getUserId();
        LinkuRequestDTO.LinkuCreateDTO linkuCreateDTO =
                LinkuConverter.toLinkuCreateDTO(linku, memo, emotionId);

        LinkuResponseDTO.LinkuCreateResult serviceResult = linkuCreateService.createLinku(userId, linkuCreateDTO, image);

        if (serviceResult.isValidUrl()) {
            return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_CREATED, serviceResult.getData());
        } else {
            return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_SUSPICIOUS_URL, serviceResult.getData());
        }
    }//linku 생성

    @Operation(
            summary = "링크 존재 여부 확인",
            description = "해당 URL의 링크가 이미 존재하는지 확인합니다."
    )
    @GetMapping("/exist")
    public ApiResponse<LinkuResponseDTO.LinkuIsExistDTO> existLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String url
    ){
        Long userId = userDetails.getUserId();
        return linkuService.existLinku(userId, url);
    }//linku 존재여부 확인

    @Operation(
            summary = "링크 상세 조회 (인증된 사용자)",
            description = "인증된 사용자의 링크 상세 정보를 조회합니다."
    )
    @GetMapping("/{linkuid}")
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable("linkuid") Long linkuid
    ){
        Long userId = userDetails.getUserId();
        return linkuService.detailGetLinku(userId, linkuid);
    } //linku 상세보기

    @Operation(
            summary = "링크 상세 조회 (사용자 ID 지정)",
            description = "특정 사용자 ID와 링크 ID로 링크 상세 정보를 조회합니다."
    )
    @GetMapping("/{userId}/{linkuId}")
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailLinku(
            @PathVariable Long userId,
            @PathVariable Long linkuId) {
        return linkuService.detailGetLinku(userId, linkuId);
    }//userId를 받아서 상세보기

    @Operation(
            summary = "최근 열람한 링크 조회",
            description = "사용자가 최근에 열람한 링크 목록을 조회합니다. limit 파라미터로 조회 개수를 지정할 수 있습니다."
    )
    @GetMapping("/recent")
    public ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> getRecentViewedLinkus(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = userDetails.getUserId();
        List<LinkuResponseDTO.LinkuSimpleDTO> result = linkuService.getRecentViewedLinkus(userId, limit);
        return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_RECENT_OK, result);
    } //최근 열람한 링크 보기

    @Operation(
            summary = "링크 수정",
            description = "기존 링크의 정보(URL, 메모, 감정, 도메인, 제목 등)를 수정합니다."
    )
    @PatchMapping(value = "/{linkuId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> updateLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long linkuId,
            @RequestBody LinkuRequestDTO.LinkuUpdateDTO updateDTO
    ) {
        Long userId = userDetails.getUserId();
        LinkuResponseDTO.LinkuResultDTO result = linkuService.updateLinku(userId, linkuId, updateDTO);
        return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_UPDATED, result);
    } //링큐 수정하기

    @Operation(
            summary = "링크 추천",
            description = "상황(situation)과 감정(emotion)을 기반으로 링크를 추천합니다. 페이지네이션을 지원합니다."
    )
    @GetMapping("/recommend")
    public ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> recommendLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam Long situationId,
            @RequestParam Long emotionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Long userId = userDetails.getUserId();
        return linkuRecommendService.recommendLinku(userId, situationId, emotionId, page, size);
    }//linku 추천 내부로

    // 빠른 검색 (사용자가 저장한 링크 전체 대상)
    @Operation(
            summary = "빠른 검색 (사용자 저장 링크 전체 대상)",
            description = "사용자가 저장한 링크 전체를 대상으로 키워드가 포함된 추천 검색어 목록을 조회합니다."
    )
    @GetMapping("/search/quick")
    public ApiResponse<List<LinkuSearchSuggestionResponse>> quickSearch(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String keyword
    ) {
        Long userId = userDetails.getUserId();
        List<LinkuSearchSuggestionResponse> result = linkuSearchService.suggest(userId, keyword);
        return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_SEARCH_OK, result);
    }

    @Operation(
            summary = "링크 삭제",
            description = "사용자가 저장한 링크를 삭제합니다."
    )
    @DeleteMapping("/{userLinkuId}")
    public ApiResponse<Object> deleteUsersLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long userLinkuId
    ) {
        Long userId = userDetails.getUserId();
        linkuService.deleteUsersLinku(userId, userLinkuId);
        return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_DELETED);

    }

}
