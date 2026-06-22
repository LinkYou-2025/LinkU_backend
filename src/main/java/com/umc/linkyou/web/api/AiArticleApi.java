package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.validation.annotation.swagger.ApiSuccessCode;
import com.umc.linkyou.web.dto.AiArticleResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ai-article-controller", description = "AI 기사 및 요약 관련 API")
public interface AiArticleApi {

    @Operation(
            summary = "AI 요약 저장 또는 조회",
            description = """
                    링크 ID에 해당하는 AI 요약 정보를 저장하거나 조회합니다.
                    - 이미 분석 결과가 존재하면 기존 데이터를 조회합니다.
                    - 데이터가 없으면 Gemini AI를 통해 분석(제목, 요약, 카테고리 등)을 수행한 후 저장합니다.
                    - Gemini AI에 의존하기 때문에 종종 Gemini가 잘못된 데이터를 반환하는 경우에 에러가 발생할 수 있습니다.
                    그럴때는 API를 다시 요청해 보세요.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    // 공통 에러 (잘못된 요청 등)
    @ApiErrorCode(errorStatus = {ErrorStatus._BAD_REQUEST})
    // 유저 관련 에러
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    // AI Article 도메인 전용 에러 (분리한 Enum 사용)
    @ApiErrorCode(aiArticleErrorStatus = {
            AiArticleErrorStatus._AI_ARTICLE_NOT_FOUND,
            AiArticleErrorStatus._AI_PARSE_ERROR,
            AiArticleErrorStatus._AI_INVALID_RESPONSE,
            AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED,
            AiArticleErrorStatus._CONTENT_EXTRACTION_PROHIBITED
    })
    @PostMapping("/{linkuid}")
    ApiResponse<AiArticleResponseDTO.AiArticleResultDTO> saveOrGetAiArticle(
            @Parameter(description = "대상 링크 ID", example = "101") @PathVariable("linkuid") Long linkuId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "마이페이지 카테고리별 AI 요약 링크 조회 (목록, 페이징)",
            description = """
                    사용자가 저장한 링크 중 AI 요약이 생성된 항목들을 카테고리별로 필터링하여 조회합니다.
                    - 커서 기반 페이징을 지원하며, 최신순으로 정렬됩니다.
                    - AI 분석 데이터가 존재하는(aiExist=true) 항목만 반환됩니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @ApiErrorCode(aiArticleErrorStatus = {AiArticleErrorStatus._CATEGORY_NOT_FOUND})
    @GetMapping("/category/{categoryId}")
    ApiResponse<LinkuResponseDTO.LinkuSliceResultDTO> getMyAiArticlesByCategory(
            @Parameter(description = """
                    조회할 카테고리 ID
                    1: 어학, 2: 뉴스, 3: 공부법, 4: IT·개발, 5: 자기계발, 6: 취업·이직, 
                    7: 비즈니스 인사이트, 8: 생산성·툴, 9: 라이프스타일, 10: 심리·자기이해, 
                    11: 에세이·칼럼, 12: 트렌드, 13: 디자인·예술, 14: 영상·뮤직, 
                    15: 맛집·여행, 16: 기타
                    """, example = "1") @PathVariable("categoryId") Long categoryId,
            @Parameter(description = "커서 ID (이전 응답의 nextCursor 값, 첫 조회 시 미포함)") @RequestParam(name = "cursor", required = false) Long cursor,
            @Parameter(description = "한 번에 조회할 개수", example = "10") @RequestParam(name = "limit", defaultValue = "10") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
