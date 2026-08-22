package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.CommonErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.validation.annotation.swagger.ApiSuccessCode;
import com.umc.linkyou.web.dto.curation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "큐레이션 API", description = "큐레이션과 관련 된 API 입니다")
@RequestMapping("/curations")
public interface CurationApi {

    @Operation(summary = "큐레이션 섹션 정보 조회", description = """
            지정한 월(YYYY-MM)의 섹션별 제목, 설명, 대표 이미지를 반환합니다.
            
            - 로그인 여부와 무관하게 모든 유저에게 동일한 응답을 반환합니다.
            - 해당 월의 큐레이션이 아직 생성되지 않은 경우, 빈 배열(`[]`)을 반환합니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(commonErrorStatus = {CommonErrorStatus._BAD_REQUEST})
    @GetMapping("/sections")
    ApiResponse<List<CurationSectionResponse>> getSectionInfo(
            @Parameter(description = "섹션 정보 월입니다. 미입력 시 이번 달을 기준으로 합니다.", example = "2025-07") @RequestParam(required = false) YearMonth month
    );

    @Operation(summary = "연도별 큐레이션 히스토리 조회", description = """
            지정한 연도의 1~12월 큐레이션 목록을 반환합니다.

            - 연도를 입력하지 않으면 올해를 기준으로 조회합니다.
            - 2025년 이전 연도를 요청하면 에러가 발생합니다.
            - 아직 생성되지 않은 달은 curationId, thumbnailUrl이 null로 반환됩니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(curationErrorStatus = {CurationErrorStatus._CURATION_INVALID_YEAR})
    @GetMapping("/history")
    ApiResponse<List<CurationListResponse>> getMyCurationList(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(required = false) Integer year
    );

    @Operation(summary = "가장 최근 큐레이션 조회", description = """
            로그인한 사용자의 가장 최근 큐레이션 정보를 조회합니다.

            - 생성된 큐레이션이 하나도 없는 경우, 200 OK와 빈 result 객체를 반환합니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @GetMapping("/latest")
    ApiResponse<Object> getLatestCuration(
            @CurrentUser CustomUserDetails userDetails
    );

    @Operation(summary = "큐레이션 상세 조회", description = """
            큐레이션 ID로 상세 정보를 조회합니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @ApiErrorCode(curationErrorStatus = {CurationErrorStatus._CURATION_NOT_FOUND, CurationErrorStatus._CURATION_FORBIDDEN})
    @GetMapping("/detail/{curationId}")
    ApiResponse<CurationDetailResponse> getCurationDetail(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long curationId
    );

    @Operation(summary = "큐레이션 기반 링크 추천", description = """
            내부 링크(4개)와 외부 링크(5개)를 반환합니다.

            - 해당 큐레이션에 아직 생성된 추천이 없으면 백그라운드로 생성을 트리거합니다.
            - 생성이 끝나기 전이라면 현재 시점에 만들어져 있는 결과만(비어있을 수 있음) 즉시 반환합니다.
            - 생성이 끝난 뒤 재요청하면 채워진 결과를 받을 수 있습니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(curationErrorStatus = {CurationErrorStatus._CURATION_NOT_FOUND, CurationErrorStatus._CURATION_FORBIDDEN})
    @GetMapping("/recommend-links")
    ApiResponse<List<RecommendedLinkResponse>> getRecommendedLinks(
            @CurrentUser CustomUserDetails userDetails,
            @Parameter(description = "추천 링크를 조회할 큐레이션 ID", example = "1") @RequestParam Long curationId
    );
}
