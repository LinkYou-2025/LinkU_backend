package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuSuccessStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.service.Linku.LinkuCreateService;
import com.umc.linkyou.service.Linku.LinkuRecommendService;
import com.umc.linkyou.service.Linku.LinkuSearchService;
import com.umc.linkyou.service.Linku.LinkuService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.LinkuApi;
import com.umc.linkyou.web.dto.linku.LinkuQuickSearchResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuSearchResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@ApiV1
@Validated
@RequiredArgsConstructor
public class LinkuController implements LinkuApi {

    private final LinkuService linkuService;
    private final LinkuCreateService linkuCreateService;
    private final LinkuSearchService linkuSearchService;
    private final LinkuRecommendService linkuRecommendService;

    @Override
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> createLinku(@CurrentUser CustomUserDetails userDetails, @RequestParam String linku, @RequestParam(required = false) String memo, @RequestParam(required = false) Long emotionId, @RequestParam(required = false) Long situationId, @RequestParam(required = false) String title, @RequestParam(required = false) MultipartFile image) {
        LinkuRequestDTO.LinkuCreateDTO linkuCreateDTO = LinkuConverter.toLinkuCreateDTO(linku, memo, emotionId, situationId, title);
        LinkuResponseDTO.LinkuCreateResult serviceResult = linkuCreateService.createLinku(userDetails.getUserId(), linkuCreateDTO, image);
        if (serviceResult.isValidUrl()) {
            return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_CREATED, serviceResult.getData());
        } else {
            return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_SUSPICIOUS_URL, serviceResult.getData());
        }
    }

    @Override
    public ApiResponse<LinkuResponseDTO.LinkuIsExistDTO> existLinku(@CurrentUser CustomUserDetails userDetails, @RequestParam String url) {
        return linkuService.existLinku(userDetails.getUserId(), url);
    }

    @Override
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailLinku(@CurrentUser CustomUserDetails userDetails, @PathVariable("linkuid") Long linkuid) {
        return linkuService.detailGetLinku(userDetails.getUserId(), linkuid);
    }

    @Override
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> detailLinku(@PathVariable Long userId, @PathVariable Long linkuId) {
        return linkuService.detailGetLinku(userId, linkuId);
    }

    @Override
    public ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> getRecentViewedLinkus(@CurrentUser CustomUserDetails userDetails, @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_RECENT_OK, linkuService.getRecentViewedLinkus(userDetails.getUserId(), limit));
    }

    @Override
    public ApiResponse<LinkuResponseDTO.LinkuResultDTO> updateLinku(@CurrentUser CustomUserDetails userDetails, @PathVariable Long linkuId, @RequestBody LinkuRequestDTO.LinkuUpdateDTO updateDTO) {
        return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_UPDATED, linkuService.updateLinku(userDetails.getUserId(), linkuId, updateDTO));
    }

    @Override
    public ApiResponse<List<LinkuResponseDTO.LinkuSimpleDTO>> recommendLinku(@CurrentUser CustomUserDetails userDetails, @RequestParam Long situationId, @RequestParam Long emotionId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        return linkuRecommendService.recommendLinku(userDetails.getUserId(), situationId, emotionId, page, size);
    }

    @Override
    public ApiResponse<LinkuSearchResponseDTO.LinkuSearchCursorPageResponse> searchLinku(@CurrentUser CustomUserDetails userDetails, @RequestParam String keyword, @RequestParam(defaultValue = "0") Long cursor, @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_SEARCH_OK, linkuSearchService.search(userDetails.getUserId(), keyword, cursor, size));
    }

    @Override
    public ApiResponse<List<LinkuQuickSearchResponseDTO>> quickSearch(@CurrentUser CustomUserDetails userDetails, @RequestParam String keyword) {
        return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_QUICK_SEARCH_OK, linkuSearchService.quickSearch(userDetails.getUserId(), keyword));
    }

    @Override
    public ApiResponse<Object> deleteUsersLinku(@CurrentUser CustomUserDetails userDetails, @PathVariable Long userLinkuId) {
        linkuService.deleteUsersLinku(userDetails.getUserId(), userLinkuId);
        return ApiResponse.onSuccess(LinkuSuccessStatus.LINKU_DELETED);
    }
}
