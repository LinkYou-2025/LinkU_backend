package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.curation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "큐레이션 API", description = "큐레이션과 관련 된 API 입니다")
@RequestMapping("/curations")
public interface CurationApi {

    @Operation(summary = "큐레이션 섹션 정보 조회", description = "지정한 월(YYYY-MM)의 섹션별 제목, 설명, 대표 이미지를 반환합니다. 미입력 시 이번 달 기준. 모든 유저 동일.")
    @GetMapping("/sections")
    ResponseEntity<ApiResponse<List<CurationSectionResponse>>> getSectionInfo(
            @RequestParam(required = false) String month
    );

    @Operation(summary = "연도별 큐레이션 히스토리 조회", description = "지정한 연도의 1~12월 큐레이션 목록을 반환합니다. 미입력 시 올해 기준. 생성되지 않은 달은 curationId, thumbnailUrl이 null입니다.")
    @GetMapping("/history")
    ResponseEntity<ApiResponse<List<CurationListResponse>>> getMyCurationList(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year
    );

    @Operation(summary = "가장 최근 큐레이션 조회", description = "내 최신 큐레이션을 조회합니다.")
    @GetMapping("/latest")
    ResponseEntity<ApiResponse<CurationLatestResponse>> getLatestCuration(
            @CurrentUser CustomUserDetails userDetails
    );

    @Operation(summary = "큐레이션 상세 조회", description = "큐레이션 ID로 상세 정보를 조회합니다.")
    @ApiErrorCode(curationErrorStatus = {CurationErrorStatus._CURATION_NOT_FOUND, CurationErrorStatus._CURATION_FORBIDDEN})
    @GetMapping("/detail/{curationId}")
    ResponseEntity<ApiResponse<CurationDetailResponse>> getCurationDetail(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long curationId
    );

    @Operation(summary = "큐레이션 기반 링크 추천", description = "내부/외부 링크를 추천합니다.")
    @ApiErrorCode(curationErrorStatus = {CurationErrorStatus._CURATION_NOT_FOUND})
    @GetMapping("/recommend-links")
    ResponseEntity<ApiResponse<List<RecommendedLinkResponse>>> getRecommendedLinks(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam Long curationId
    );
}
