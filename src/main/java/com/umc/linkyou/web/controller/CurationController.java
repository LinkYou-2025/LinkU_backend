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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@ApiV1
@RequiredArgsConstructor
public class CurationController implements CurationApi {

    private final CurationService curationService;
    private final CurationRecommendBuilderService curationRecommendBuilderService;

    @Override
    public ResponseEntity<ApiResponse<List<CurationSectionResponse>>> getSectionInfo(@RequestParam(required = false) YearMonth month) {
        YearMonth resolvedMonth = (month != null) ? month : YearMonth.now();
        return ResponseEntity.ok(ApiResponse.onSuccess(curationService.getCurationSections(resolvedMonth.toString())));
    }

    @Override
    public ResponseEntity<ApiResponse<List<CurationListResponse>>> getMyCurationList(@CurrentUser CustomUserDetails userDetails, @RequestParam(required = false) Integer year) {
        int resolvedYear = (year != null) ? year : YearMonth.now().getYear();
        return ResponseEntity.ok(ApiResponse.onSuccess(curationService.getCurationList(userDetails.getUserId(), resolvedYear)));
    }

    @Override
    public ResponseEntity<ApiResponse<CurationLatestResponse>> getLatestCuration(@CurrentUser CustomUserDetails userDetails) {
        return curationService.getLatestCuration(userDetails.getUserId())
                .map(body -> ResponseEntity.ok(ApiResponse.onSuccess(body)))
                .orElse(ResponseEntity.noContent().build());
    }

    @Override
    public ResponseEntity<ApiResponse<CurationDetailResponse>> getCurationDetail(@CurrentUser CustomUserDetails userDetails, @PathVariable Long curationId) {
        return ResponseEntity.ok(ApiResponse.onSuccess(curationService.getCurationDetail(userDetails.getUserId(), curationId)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<RecommendedLinkResponse>>> getRecommendedLinks(@CurrentUser CustomUserDetails userDetails, @RequestParam Long curationId) {
        List<RecommendedLinkResponse> recommendations = curationRecommendBuilderService.buildRecommendedLinks(userDetails.getUserId(), curationId);
        return ResponseEntity.ok(ApiResponse.onSuccess(recommendations));
    }
}
