package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.validation.annotation.swagger.ApiSuccessCode;
import com.umc.linkyou.web.dto.AiArticleResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ai-article-controller", description = "AI 기사 및 요약 관련 API")
public interface AiArticleApi {

    @Operation(
            summary = "AI 요약 저장 또는 조회",
            description = """
                    - 해당 링크에 대한 AI 분석 결과(`ai_articles`)가 이미 존재하고 summary가 채워져 있으면, 새로 분석하지 않고 기존 데이터를 그대로 반환합니다.
                    - 분석이 완료되면 `LINK_SUMMARY_COMPLETE` 알림이 발송됩니다.

                   - 크롤링 대상 사이트의 robots.txt가 크롤링을 금지하는 경우 403(`CRAWLER4031`)이 반환되며, 이 경우 재시도해도 결과는 같습니다.
                    - 그 외 크롤링 자체가 실패(네트워크 오류, 파싱 실패 등)하거나 Gemini 응답이 예상 형식이 아닌 경우 500이 반환될 수 있습니다. 이런 경우는 일시적인 문제일 수 있으니 API를 다시 요청해 보세요.
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
    @PostMapping("/linku/aiarticle/{userLinkuId}")
    ApiResponse<AiArticleResponseDTO.AiArticleResultDTO> saveOrGetAiArticle(
            @Parameter(description = "AI 요약을 저장/조회할 대상 사용자 링크 ID", example = "101") @PathVariable Long userLinkuId,
            @CurrentUser CustomUserDetails userDetails
    );

    @Operation(
            summary = "마이페이지 카테고리별 AI 요약 링크 조회 (목록, 페이징)",
            description = """
                    사용자가 저장한 링크 중 AI 요약이 생성된(`aiExist=true`) 항목들을, 카테고리로 필터링하여 목록으로 조회합니다. 마이페이지의 "AI 요약 글 보기" 화면에서 사용됩니다.

                    **페이징 방식**
                    - `userLinkuId` 기준 커서 페이징이며, 최신 저장 순(내림차순)으로 정렬됩니다.
                    - 첫 요청 시 `cursor` 파라미터를 생략합니다.
                    - 응답의 `hasNext=true`이면, 응답의 `nextCursor` 값을 다음 요청의 `cursor`로 그대로 전달해 다음 페이지를 이어서 조회합니다.
                    - `hasNext=false`이면 `nextCursor`는 null이며 더 이상 가져올 데이터가 없다는 뜻입니다.

                    **응답 항목 (linkuList의 각 원소)**
                    - `linkuId`, `linku`(원본 URL), `emotionId`, `domain`(도메인명), `domainImageUrl`, `title`, `linkuImageUrl`, `categoryId`, `categoryName`을 반환합니다.
                    - `categoryId`/`categoryName`은 "전체" 탭처럼 여러 카테고리가 섞여 조회될 때 각 항목이 어느 카테고리인지 구분하기 위해 항상 포함됩니다.

                    **전체 카테고리 조회**
                    - `categoryId`를 생략하면 카테고리 필터 없이 전체 카테고리의 AI 요약 링크를 조회합니다("전체" 탭).
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @ApiErrorCode(aiArticleErrorStatus = {AiArticleErrorStatus._CATEGORY_NOT_FOUND})
    @GetMapping
    ApiResponse<LinkuResponseDTO.LinkuSliceResultDTO> getMyAiArticlesByCategory(
            @Parameter(description = """
                    조회할 카테고리 ID. 생략하면 전체 카테고리를 조회합니다("전체" 탭).
                    1: 어학, 2: 뉴스, 3: 공부법, 4: IT·개발, 5: 자기계발, 6: 취업·이직,
                    7: 비즈니스 인사이트, 8: 생산성·툴, 9: 라이프스타일, 10: 심리·자기이해,
                    11: 에세이·칼럼, 12: 트렌드, 13: 디자인·예술, 14: 영상·뮤직,
                    15: 맛집·여행, 16: 기타
                    """, example = "1", required = false) @RequestParam(name = "categoryId", required = false) Long categoryId,
            @Parameter(description = "커서 ID. 이전 응답의 nextCursor 값을 그대로 전달하며, 첫 조회 시에는 생략합니다.") @RequestParam(name = "cursor", required = false) Long cursor,
            @Parameter(description = "한 번에 조회할 개수", example = "10") @RequestParam(name = "limit", defaultValue = "10") int limit,
            @CurrentUser CustomUserDetails userDetails
    );
}
