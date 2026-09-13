package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.curation.CurationService;
import com.umc.linkyou.service.curation.recommend.CurationRecommendBuilderService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.CurationApi;
import com.umc.linkyou.web.dto.curation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

@ApiV1
@RequiredArgsConstructor
public class CurationController implements CurationApi {

    private final CurationService curationService;
    private final CurationRecommendBuilderService curationRecommendBuilderService;

    @Override
    public ApiResponse<List<CurationSectionResponse>> getSectionInfo(@RequestParam(required = false) YearMonth month) {
        YearMonth resolvedMonth = (month != null) ? month : YearMonth.now();
        return ApiResponse.onSuccess(curationService.getCurationSections(resolvedMonth.toString()));
    }

    @Override
    public ApiResponse<List<CurationListResponse>> getMyCurationList(@CurrentUser CustomUserDetails userDetails, @RequestParam(required = false) Integer year) {
        int resolvedYear = (year != null) ? year : YearMonth.now().getYear();
        return ApiResponse.onSuccess(curationService.getCurationList(userDetails.getUserId(), resolvedYear));
    }

    @Override
    public ApiResponse<Object> getLatestCuration(@CurrentUser CustomUserDetails userDetails) {
        Object curation = curationService.getLatestCuration(userDetails.getUserId())
                .<Object>map(response -> response)
                .orElseGet(Collections::emptyMap);
        return ApiResponse.onSuccess(curation);
    }

    @Override
    public ApiResponse<CurationDetailResponse> getCurationDetail(@CurrentUser CustomUserDetails userDetails, @PathVariable Long curationId) {
        return ApiResponse.onSuccess(curationService.getCurationDetail(userDetails.getUserId(), curationId));
    }

    @Override
    public ApiResponse<List<RecommendedLinkResponse>> getRecommendedLinks(@CurrentUser CustomUserDetails userDetails, @RequestParam Long curationId) {
        List<RecommendedLinkResponse> recommendations = curationRecommendBuilderService.buildRecommendedLinks(userDetails.getUserId(), curationId);
        return ApiResponse.onSuccess(recommendations);
    }
}
