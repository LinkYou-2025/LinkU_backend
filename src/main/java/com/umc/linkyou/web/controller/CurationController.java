package com.umc.linkyou.web.controller;

import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.service.curation.CurationService;
import com.umc.linkyou.service.curation.recommend.CurationRecommendBuilderService;
import com.umc.linkyou.web.dto.curation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "curation-controller", description = "큐레이션 관련 API")
@ApiV1
@RequiredArgsConstructor
@RequestMapping("/curations")
public class CurationController {

    private final CurationService curationService;
    private final CurationRecommendBuilderService curationRecommendBuilderService;

    // 월별 섹션 정보 조회 (제목, 설명, 대표 이미지)
    @Operation(
            summary = "큐레이션 섹션 정보 조회",
            description = "지정한 월(YYYY-MM)의 섹션별 제목, 설명, 대표 이미지를 반환합니다. 미입력 시 이번 달 기준. 모든 유저 동일.")
    @GetMapping("/sections")
    public ResponseEntity<ApiResponse<List<CurationSectionResponse>>> getSectionInfo(
            @RequestParam(required = false) String month
    ) {
        String resolvedMonth = (month != null) ? month : YearMonth.now().toString();
        return ResponseEntity.ok(ApiResponse.onSuccess(curationService.getCurationSections(resolvedMonth)));
    }

    // 올해 12개 큐레이션 히스토리 (없는 달은 빈 상태)
    @Operation(
            summary = "연도별 큐레이션 히스토리 조회",
            description = "지정한 연도의 1~12월 큐레이션 목록을 반환합니다. 미입력 시 올해 기준. 생성되지 않은 달은 curationId, thumbnailUrl이 null입니다.")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<CurationListResponse>>> getMyCurationList(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year
    ) {
        Long userId = userDetails.getUserId();
        int resolvedYear = (year != null) ? year : YearMonth.now().getYear();
        return ResponseEntity.ok(ApiResponse.onSuccess(curationService.getCurationList(userId, resolvedYear)));
    }

    // 가장 최근 큐레이션 조회
    @Operation(
            summary = "가장 최근 큐레이션 조회",
            description = "내 최신 큐레이션을 조회합니다.")
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<CurationLatestResponse>> getLatestCuration(
            @CurrentUser CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        return curationService.getLatestCuration(userId)
                .map(body -> ResponseEntity.ok(ApiResponse.onSuccess(body)))
                .orElse(ResponseEntity.noContent().build());
    }

    // 큐레이션 상세 조회
    @Operation(
            summary = "큐레이션 상세 조회",
            description = "큐레이션 ID로 상세 정보를 조회합니다.")
    @GetMapping("/detail/{curationId}")
    public ResponseEntity<ApiResponse<CurationDetailResponse>> getCurationDetail(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long curationId) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.onSuccess(curationService.getCurationDetail(userId, curationId)));
    }

    // 큐레이션 링크 추천
    @Operation(
            summary = "큐레이션 기반 링크 추천",
            description = "내부/외부 링크를 추천합니다."
    )
    @GetMapping("/recommend-links")
    public ResponseEntity<ApiResponse<List<RecommendedLinkResponse>>> getRecommendedLinks(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam Long curationId
    ) {
        Long userId = userDetails.getUserId();
        List<RecommendedLinkResponse> recommendations = curationRecommendBuilderService.buildRecommendedLinks(userId, curationId);
        return ResponseEntity.ok(ApiResponse.onSuccess(recommendations));
    }
}