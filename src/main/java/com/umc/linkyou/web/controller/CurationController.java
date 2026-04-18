package com.umc.linkyou.web.controller;

import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.ApiV1;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.service.curation.CurationService;
import com.umc.linkyou.service.curation.linku.CurationRecommendBuilderService;
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

    // [관리용] 전체 유저 월간 큐레이션 즉시 생성
    @Operation(
            summary = "전체 유저 월간 큐레이션 생성",
            description = "[관리용] 전월 기준으로 모든 유저의 큐레이션을 즉시 생성합니다.")
    @PostMapping("/batch/manual")
    public ResponseEntity<ApiResponse<Void>> triggerBatch() {
        curationService.generateMonthlyCurationForAllUsers();
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    // [테스트용] 특정 유저·월 큐레이션 즉시 생성
    @Operation(
            summary = "단일 유저 큐레이션 생성",
            description = "[테스트용] userId와 month(YYYY-MM)를 지정해 큐레이션을 즉시 생성합니다.")
    @PostMapping("/batch/manual/test")
    public ResponseEntity<ApiResponse<Void>> triggerBatchForUser(
            @RequestParam Long userId,
            @RequestParam String month
    ) {
        curationService.generateCurationForUser(userId, month);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    // 월별 섹션 정보 조회 (제목, 설명, 대표 이미지)
    @Operation(
            summary = "큐레이션 섹션 정보 조회",
            description = "지정한 월(YYYY-MM)의 섹션별 제목, 설명, 대표 이미지를 반환합니다. 미입력 시 이번 달 기준. 모든 유저 동일.")
    @GetMapping("/sections")
    public ResponseEntity<ApiResponse<List<CurationSectionResponse>>> getSectionInfo(
            @RequestParam(required = false) String month
    ) {
        String resolvedMonth = (month != null) ? month : YearMonth.now().toString();
        return ResponseEntity.ok(ApiResponse.onSuccess(curationService.getSectionInfo(resolvedMonth)));
    }

    // 올해 12개 큐레이션 히스토리 (없는 달은 빈 상태)
    @Operation(
            summary = "연도별 큐레이션 히스토리 조회",
            description = "지정한 연도의 1~12월 큐레이션 목록을 반환합니다. 미입력 시 올해 기준. 생성되지 않은 달은 curationId, thumbnailUrl이 null입니다.")
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<CurationListResponse>>> getMyCurationList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year
    ) {
        Long userId = userDetails.getUsers().getId();
        int resolvedYear = (year != null) ? year : YearMonth.now().getYear();
        return ResponseEntity.ok(ApiResponse.onSuccess(curationService.getMyCurationList(userId, resolvedYear)));
    }

    // 가장 최근 큐레이션 조회
    @Operation(
            summary = "가장 최근 큐레이션 조회",
            description = "내 최신 큐레이션을 조회합니다. 큐레이션이 없으면 204(No Content)를 반환합니다.")
    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<CurationLatestResponse>> getLatestCuration(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long userId = userDetails.getUsers().getId();
        return curationService.getLatestCuration(userId)
                .map(body -> ResponseEntity.ok(ApiResponse.onSuccess(body)))
                .orElse(ResponseEntity.noContent().build());
    }

    // 큐레이션 상세 조회
    @Operation(
            summary = "큐레이션 상세 조회",
            description = "큐레이션 ID로 상세 정보를 조회합니다. 본인 큐레이션만 조회 가능합니다.")
    @GetMapping("/detail/{curationId}")
    public ResponseEntity<ApiResponse<CurationDetailResponse>> getCurationDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long curationId) {
        Long userId = userDetails.getUsers().getId();
        return ResponseEntity.ok(ApiResponse.onSuccess(curationService.getCurationDetail(userId, curationId)));
    }

    // 큐레이션 링크 추천
    @Operation(
            summary = "큐레이션 기반 링크 추천",
            description = "해당 큐레이션을 기반으로 내부/외부 추천 로직을 종합하여 링크를 추천합니다."
    )
    @GetMapping("/recommend-links")
    public ResponseEntity<ApiResponse<List<RecommendedLinkResponse>>> getRecommendedLinks(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam Long curationId
    ) {
        Long userId = userDetails.getUsers().getId();
        var recommendations = curationRecommendBuilderService.buildRecommendedLinks(userId, curationId);
        return ResponseEntity.ok(ApiResponse.onSuccess(recommendations));
    }
}