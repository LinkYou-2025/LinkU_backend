package com.umc.linkyou.web.controller;

import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.ApiV1;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.umc.linkyou.web.dto.curation.CurationListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "curation-controller", description = "큐레이션 관련 API")
@ApiV1
@RestController
@RequiredArgsConstructor
@RequestMapping("/curations")
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
     * [기존] 가장 최근 큐레이션 조회
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
     * [수정] 내 큐레이션 히스토리 (전체보기 + 페이징)
     */
    @Operation(
            summary = "내 큐레이션 전체 히스토리 조회",
            description = "나의 월별 큐레이션 전체 목록을 최신순으로 페이징하여 조회합니다. (size 기본값: 10)"
    )
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<CurationListResponse>>> getMyCurationList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Long userId = userDetails.getUsers().getId();

        // 0페이지부터 시작, size 개수만큼 가져오기
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<CurationListResponse> result = curationService.getMyCurationList(userId, pageRequest);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
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
     * [기존] 큐레이션 좋아요 리스트 가져오기
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
     * [수정] 좋아요한 큐레이션 전체 리스트 (전체보기 + 페이징)
     */
    @Operation(
            summary = "좋아요한 큐레이션 전체 조회",
            description = "내가 좋아요를 누른 큐레이션 전체 목록을 좋아요 누른 최신순으로 페이징하여 조회합니다."
    )
    @GetMapping("/likes")
    public ResponseEntity<ApiResponse<Page<CurationListResponse>>> getLikedCurationList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Long userId = userDetails.getUsers().getId();

        PageRequest pageRequest = PageRequest.of(page, size);
        Page<CurationListResponse> result = curationLikeService.getLikedCurationList(userId, pageRequest);
        return ResponseEntity.ok(ApiResponse.onSuccess(result));
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

    /**
     * [2 페이지] 이번 달 많이 본 키워드 조회 (워드클라우드용)
     */
    @Operation(
            summary = "Get Monthly Top Keywords",
            description = "이번 달에 유저가 가장 많이 열람한 AI 키워드 랭킹을 조회합니다.<br>" +
                    "결과값의 `viewCount`에 비례하여 워드클라우드의 글자 크기나 색상을 렌더링해 주세요."
    )
    @GetMapping("/monthly-keywords")
    public ApiResponse<List<CurationAnalyticsDTO.KeywordCountResponse>> getMonthlyTopKeywords(
            @RequestParam("userId") Long userId
    ) {
        List<CurationAnalyticsDTO.KeywordCountResponse> result = curationService.getMonthlyTopKeywords(userId);
        return ApiResponse.onSuccess("이번 달 많이 본 키워드 조회 성공", result);
    }

    /**
     * [2페이지 연계] 특정 키워드 클릭 시 해당 링크 리스트 조회 (스마트 라우팅용)
     * 프론트엔드에서 결과 리스트 길이가 1개면 바로 외부링크로 이동, 2개 이상이면 리스트 화면을 보여줍니다.
     */
    @Operation(
            summary = "Get Links by Keyword (Smart Routing)",
            description = "워드클라우드에서 특정 키워드를 클릭했을 때 해당 키워드가 포함된 링크 리스트를 최신순으로 조회합니다.<br>" +
                    "<b>[프론트엔드 라우팅 필수]</b><br>" +
                    "- 결과 리스트 길이가 `1`인 경우: 리스트 화면으로 이동하지 않고 즉시 해당 객체의 `url`을 새 창으로 띄워주세요.<br>" +
                    "- 결과 리스트 길이가 `2` 이상인 경우: 키워드 링크 리스트 화면으로 이동하여 리스트를 렌더링해 주세요."
    )
    @GetMapping("/keyword-links")
    public ApiResponse<List<CurationAnalyticsDTO.KeywordLinkResponse>> getLinksByKeyword(
            @Parameter(description = "사용자 ID", required = true) @RequestParam("userId") Long userId,
            @Parameter(description = "조회할 AI 키워드", required = true) @RequestParam("keyword") String keyword
    ) {
        List<CurationAnalyticsDTO.KeywordLinkResponse> result = curationService.getLinksByKeyword(userId, keyword);
        return ApiResponse.onSuccess("키워드 관련 링크 조회 성공", result);
    }

    /**
     * [3-3] 지난달 저장만 하고 한 번도 안 본 링크 조회
     */
    @Operation(
            summary = "Get Unread Links of Last Month",
            description = "지난달에 저장한 링크 중, 단 한 번도 열람(클릭)하지 않은 링크 리스트를 저장한 순서대로 조회합니다."
    )
    @GetMapping("/unread-links")
    public ApiResponse<List<CurationAnalyticsDTO.UnreadLinkResponse>> getLastMonthUnreadLinks(
            @Parameter(description = "사용자 ID", required = true) @RequestParam("userId") Long userId
    ) {
        List<CurationAnalyticsDTO.UnreadLinkResponse> result = curationService.getLastMonthUnreadLinks(userId);
        return ApiResponse.onSuccess("안 본 링크 조회 성공", result);
    }
}