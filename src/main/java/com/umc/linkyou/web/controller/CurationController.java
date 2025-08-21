package com.umc.linkyou.web.controller;

import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.service.curation.CurationLikeService;
import com.umc.linkyou.service.curation.CurationService;
import com.umc.linkyou.service.curation.CurationTopLogService;
import com.umc.linkyou.service.curation.linku.CurationRecommendBuilderService;
import com.umc.linkyou.service.curation.linku.ExternalRecommendService;
import com.umc.linkyou.service.curation.linku.InternalLinkCandidateService;
import com.umc.linkyou.web.dto.curation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "curation-controller", description = "큐레이션 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/curations")
public class CurationController {

    private final CurationTopLogService curationTopLogService;
    private final CurationService curationService;
    private final CurationLikeService curationLikeService;
    private final CurationRecommendBuilderService curationRecommendBuilderService;
    private final InternalLinkCandidateService internalLinkCandidateService;

    // 자동생성 테스트
    @Operation(
            summary = "배치 트리거(관리용)",
            description = "모든 사용자에 대해 월간 큐레이션을 즉시 생성합니다. 운영/개발 전용 엔드포인트입니다."
    )
    @GetMapping("/batch/manual")
    public ResponseEntity<ApiResponse<Void>> triggerBatch() {
        curationService.generateMonthlyCurationForAllUsers();
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    @Operation(
            summary = "개발용 시드: 2025-02 ~ 2025-07 큐레이션 생성",
            description = "기존 운영 코드 변경 없이, 테스트 데이터만 일괄 생성합니다. 이미 존재하는 (user, month)는 스킵합니다."
    )
    @PostMapping("/seed-feb-to-jul-2025")
    public ResponseEntity<ApiResponse<Void>> seedFebToJul2025(
            @RequestParam(defaultValue = "false") boolean materializeExternal
    ) {
        curationService.seedFebToJul2025(materializeExternal);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    /**
     * 큐레이션 상세 조회 API
     */
    @Operation(
            summary = "큐레이션 상세 조회",
            description = "큐레이션 ID로 상세 정보를 조회합니다."
    )
    @GetMapping("/detail/{curationId}")
    public ResponseEntity<ApiResponse<CurationDetailResponse>> getCurationDetail(@PathVariable Long curationId) {
        CurationDetailResponse response = curationService.getCurationDetail(curationId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 가장 최근 큐레이션 조회
     */
    @Operation(
            summary = "가장 최근 큐레이션 조회",
            description = "사용자 ID로 해당 사용자의 최신 큐레이션을 조회합니다. 없으면 204(No Content) 반환."
    )
    @GetMapping("/latest/{userId}")
    public ResponseEntity<ApiResponse<CurationLatestResponse>> getLatestCuration(@PathVariable Long userId) {
        var body = curationService.getLatestCuration(userId).orElse(null);
        return ResponseEntity.ok(ApiResponse.onSuccess(body));
    }

    /**
     * 큐레이션 좋아요 등록
     */
    @Operation(
            summary = "큐레이션 좋아요 등록",
            description = "해당 큐레이션에 좋아요를 등록합니다."
    )
    @PostMapping("/{curationId}/like")
    public ResponseEntity<ApiResponse<Void>> likeCuration(@PathVariable Long curationId, @RequestParam Long userId) {
        curationLikeService.likeCuration(userId, curationId);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }
    /**
     * 큐레이션 좋아요 취소
     */
    @Operation(
            summary = "큐레이션 좋아요 취소",
            description = "해당 큐레이션의 좋아요를 취소합니다."
    )
    @DeleteMapping("/{curationId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeCuration(@PathVariable Long curationId, @RequestParam Long userId) {
        curationLikeService.unlikeCuration(userId, curationId);
        return ResponseEntity.ok(ApiResponse.onSuccess(null));
    }

    /**
     * 큐레이션 좋아요 여부 확인
     */
    @Operation(
            summary = "큐레이션 좋아요 여부 조회",
            description = "해당 큐레이션에 사용자가 좋아요를 눌렀는지 여부를 조회합니다."
    )
    @GetMapping("/{curationId}/like")
    public ResponseEntity<ApiResponse<CurationLikeStatusResponse>> isLiked(
            @PathVariable Long curationId,
            @RequestParam Long userId
    ) {
        boolean liked = curationLikeService.isLiked(userId, curationId);
        return ResponseEntity.ok(ApiResponse.onSuccess(new CurationLikeStatusResponse(liked)));
    }

    /**
     * 큐레이션 좋아요 리스트 가져오기
     */
    @Operation(
            summary = "최근 좋아요한 큐레이션 목록",
            description = "사용자의 최근 좋아요 기록을 최신순으로 조회합니다."
    )
    @GetMapping("/likes/recent")
    public ResponseEntity<ApiResponse<List<LikedCurationResponse>>> getRecentLikedCurations(@RequestParam Long userId) {
        var list = curationLikeService.getRecentLikedCurations(userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(list));
    }

    /**
     * 큐레이션 링크 추천
     */
    @Operation(
            summary = "큐레이션 기반 링크 추천",
            description = "해당 큐레이션을 기반으로 내부/외부 추천 로직을 종합하여 링크를 추천합니다."
    )
    @GetMapping("/recommend-links")
    public ResponseEntity<ApiResponse<List<RecommendedLinkResponse>>> getRecommendedLinks(
            @RequestParam Long userId,
            @RequestParam Long curationId
    ) {
        var recommendations = curationRecommendBuilderService.buildRecommendedLinks(userId, curationId);
        return ResponseEntity.ok(ApiResponse.onSuccess(recommendations));
    }


    /**
     * 내부 링크 유사도 상위 2개
     */
    @Operation(
            summary = "내부 유사 링크 상위 2개",
            description = "내부 보유 링크 중 해당 큐레이션과 유사도가 높은 상위 2개 링크를 조회합니다."
    )
    @GetMapping("/recommend-links/internal/top2")
    public ResponseEntity<ApiResponse<List<RecommendedLinkResponse>>> getInternalSimilarLinks(
            @RequestParam Long userId,
            @RequestParam Long curationId
    ) {
        var result = internalLinkCandidateService.getTop2SimilarInternalLinks(userId, curationId);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
    }
}