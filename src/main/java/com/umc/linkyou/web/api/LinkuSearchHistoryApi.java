package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.CommonErrorStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.linku.LinkuSearchHistoryItemDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "search-history-controller", description = "검색어 기록 관련 API")
@RequestMapping("/links/search/history")
public interface LinkuSearchHistoryApi {

    @Operation(summary = "최근 검색어 조회")
    @ApiErrorCode(
            commonErrorStatus = {CommonErrorStatus._UNAUTHORIZED},
            userErrorStatus = {UserErrorStatus._USER_NOT_FOUND}
    )
    @GetMapping
    ApiResponse<List<LinkuSearchHistoryItemDTO>> getRecentKeywords(
            @CurrentUser CustomUserDetails userDetails
    );

    @Operation(summary = "검색어 단일 삭제")
    @ApiErrorCode(
            commonErrorStatus = {CommonErrorStatus._UNAUTHORIZED},
            userErrorStatus = {UserErrorStatus._USER_NOT_FOUND},
            linkuErrorStatus = {LinkuErrorStatus._SEARCH_HISTORY_NOT_FOUND}
    )
    @DeleteMapping("/{searchHistoryId}")
    ApiResponse<Object> deleteKeyword(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long searchHistoryId
    );

    @Operation(summary = "검색어 전체 삭제")
    @ApiErrorCode(
            commonErrorStatus = {CommonErrorStatus._UNAUTHORIZED},
            userErrorStatus = {UserErrorStatus._USER_NOT_FOUND},
            linkuErrorStatus = {LinkuErrorStatus._SEARCH_HISTORY_NOT_FOUND}
    )
    @DeleteMapping
    ApiResponse<Object> deleteAllKeywords(
            @CurrentUser CustomUserDetails userDetails
    );
}
