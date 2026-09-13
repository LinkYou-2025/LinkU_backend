package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuSuccessStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.Linku.LinkuSearchService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.LinkuSearchHistoryApi;
import com.umc.linkyou.web.dto.linku.LinkuSearchHistoryItemDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ApiV1
@RequiredArgsConstructor
public class LinkuSearchHistoryController implements LinkuSearchHistoryApi {

    private final LinkuSearchService linkuSearchService;

    @Override
    public ApiResponse<List<LinkuSearchHistoryItemDTO>> getRecentKeywords(
            @CurrentUser CustomUserDetails userDetails) {
        return ApiResponse.onSuccess(
                LinkuSuccessStatus.SEARCH_HISTORY_OK,
                linkuSearchService.getRecentKeywords(userDetails.getUserId()));
    }

    @Override
    public ApiResponse<Object> deleteKeyword(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long searchHistoryId) {
        linkuSearchService.deleteKeyword(userDetails.getUserId(), searchHistoryId);
        return ApiResponse.onSuccess(LinkuSuccessStatus.SEARCH_HISTORY_DELETED);
    }

    @Override
    public ApiResponse<Object> deleteAllKeywords(
            @CurrentUser CustomUserDetails userDetails) {
        linkuSearchService.deleteAllKeywords(userDetails.getUserId());
        return ApiResponse.onSuccess(LinkuSuccessStatus.SEARCH_HISTORY_ALL_DELETED);
    }
}
