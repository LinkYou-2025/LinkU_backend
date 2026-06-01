package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchSuggestionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "linku-controller", description = "링크(Linku) 관련 API")
@RequestMapping("/linku")
public interface LinkuApi {

    @Operation(summary = "링크 생성", description = "새로운 링크를 생성합니다. URL, 메모, 감정 ID, 이미지를 포함할 수 있습니다.")
    @ApiErrorCode(errorStatus = {ErrorStatus._LINKU_INVALID_URL, ErrorStatus._LINKU_VIDEO_NOT_ALLOWED})
    @PostMapping(value = "", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<LinkuResponseDTO.LinkuResultDTO> createLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String linku,
            @RequestParam(required = false) String memo,
            @RequestParam(required = false) Long emotionId,
            @RequestParam(required = false) MultipartFile image
    );

    @Operation(summary = "링크 존재 여부 확인", description = "해당 URL의 링크가 이미 존재하는지 확인합니다.")
    @GetMapping("/exist")
    ApiResponse<LinkuResponseDTO.LinkuIsExistDTO> existLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String url
    );

    @Operation(summary = "링크 상세 조회 (인증된 사용자)", description = "인증된 사용자의 링크 상세 정보를 조회합니다.")
    @ApiErrorCode(errorStatus = {ErrorStatus._LINKU_NOT_FOUND, ErrorStatus._USER_LINKU_NOT_FOUND})
    @GetMapping("/{linkuid}")
    ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable("linkuid") Long linkuid
    );

    @Operation(summary = "링크 상세 조회 (사용자 ID 지정)", description = "특정 사용자 ID와 링크 ID로 링크 상세 정보를 조회합니다.")
    @ApiErrorCode(errorStatus = {ErrorStatus._LINKU_NOT_FOUND, ErrorStatus._USER_LINKU_NOT_FOUND})
    @GetMapping("/{userId}/{linkuId}")
    ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailLinku(
            @PathVariable Long userId,
            @PathVariable Long linkuId
    );

    @Operation(summary = "최근 열람한 링크 조회", description = "사용자가 최근에 열람한 링크 목록을 조회합니다. limit 파라미터로 조회 개수를 지정할 수 있습니다.")
    @GetMapping("/recent")
    ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> getRecentViewedLinkus(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(defaultValue = "10") int limit
    );

    @Operation(summary = "링크 수정", description = "기존 링크의 정보(URL, 메모, 감정, 도메인, 제목 등)를 수정합니다.")
    @ApiErrorCode(errorStatus = {ErrorStatus._LINKU_NOT_FOUND, ErrorStatus._USER_LINKU_NOT_FOUND})
    @PatchMapping(value = "/{linkuId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<LinkuResponseDTO.LinkuResultDTO> updateLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long linkuId,
            @RequestBody LinkuRequestDTO.LinkuUpdateDTO updateDTO
    );

    @Operation(summary = "링크 추천", description = "상황(situation)과 감정(emotion)을 기반으로 링크를 추천합니다. 페이지네이션을 지원합니다.")
    @ApiErrorCode(errorStatus = {ErrorStatus._SITUATION_NOT_FOUND, ErrorStatus._EMOTION_NOT_FOUND, ErrorStatus._RECOMMEND_LINKU_NOT_ENOUGH_LINKS, ErrorStatus._RECOMMEND_LINKU_NO_RECOMMENDATION, ErrorStatus._RECOMMEND_LINKU_NEW_USER})
    @GetMapping("/recommend")
    ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> recommendLinku(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam Long situationId,
            @RequestParam Long emotionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    );

    @Operation(summary = "빠른 검색 (사용자 저장 링크 전체 대상)", description = "사용자가 저장한 링크 전체를 대상으로 키워드가 포함된 추천 검색어 목록을 조회합니다.")
    @GetMapping("/search/quick")
    ApiResponse<List<LinkuSearchSuggestionResponse>> quickSearch(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String keyword
    );

    @Operation(summary = "링크 삭제", description = "사용자가 저장한 링크를 삭제합니다.")
    @ApiErrorCode(errorStatus = {ErrorStatus._USER_LINKU_NOT_FOUND})
    @DeleteMapping("/{userLinkuId}")
    ApiResponse<Object> deleteUsersLinku(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long userLinkuId
    );
}
