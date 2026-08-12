package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.code.status.linku.LinkuErrorStatus;
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
            summary = "AI 요약 조회",
            description = """
                    해당 링크에 대한 AI 요약(`ai_articles`) 레코드를 조회합니다. 생성은 하지 않습니다 — 레코드가 아예 없으면
                    404(`AIARTICLE4041`)를 반환하며, 이 경우에만 `POST /aiarticle/{linkuid}`로 생성을 요청하세요.

                    응답의 `status` 필드로 진행 상태를 판단합니다.
                    - `PENDING`: 생성 진행 중. summary는 아직 null이며, 프론트는 일정 간격으로 이 GET을 다시 호출(폴링)합니다.
                    - `DONE`: 생성 완료. summary/tags 등을 그대로 표시합니다.
                    - `FAILED`: 생성 실패. `failReason`에 실패 사유 코드가 담깁니다(예: `CRAWLER4031` - robots.txt 차단,
                      재시도해도 동일한 사유로 다시 실패합니다). `POST`를 다시 호출하면 재시도합니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(errorStatus = {ErrorStatus._BAD_REQUEST})
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @ApiErrorCode(linkuErrorStatus = {LinkuErrorStatus._USER_LINKU_NOT_FOUND})
    @ApiErrorCode(aiArticleErrorStatus = {AiArticleErrorStatus._AI_ARTICLE_NOT_FOUND})
    @GetMapping("/{linkuid}")
    ApiResponse<AiArticleResponseDTO.AiArticleResultDTO> getAiArticle(
            @Parameter(description = "AI 요약을 조회할 대상 링크 ID (Linku 엔티티의 linkuId). 요청 유저가 저장한 링크여야 합니다.", example = "101") @PathVariable("linkuid") Long linkuId,
            @CurrentUser CustomUserDetails userDetails
    );

    @Operation(
            summary = "AI 요약 생성 요청",
            description = """
                    해당 링크에 대한 AI 요약 생성을 시작합니다. 크롤링/Gemini 호출을 기다리지 않고 즉시
                    status=`PENDING` 응답을 반환합니다 — 실제 완료 여부는 `GET /aiarticle/{linkuid}`를 폴링해 확인하세요.

                    - 이미 생성이 완료(`DONE`)된 링크에 다시 요청하면 409(`AIARTICLE4091`)가 반환됩니다. `GET`으로 조회하세요.
                    - 이미 생성이 진행 중(`PENDING`)인 링크에 다시 요청하면 409(`AIARTICLE4092`)가 반환됩니다.
                    - 이전 시도가 실패(`FAILED`)했던 링크는 재시도로 간주되어 다시 `PENDING`으로 생성을 시작합니다.
                    - robots.txt 차단, 크롤링 실패, Gemini 오류 등 생성 자체의 실패는 이 API의 에러가 아니라, 이후
                      `GET`에서 status=`FAILED`와 `failReason`으로 나타납니다.
                    """
    )
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(errorStatus = {ErrorStatus._BAD_REQUEST})
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @ApiErrorCode(linkuErrorStatus = {LinkuErrorStatus._USER_LINKU_NOT_FOUND})
    @ApiErrorCode(aiArticleErrorStatus = {
            AiArticleErrorStatus._DUPLICATE_AI_ARTICLE,
            AiArticleErrorStatus._AI_ARTICLE_GENERATING
    })
    @PostMapping("/{linkuid}")
    ApiResponse<AiArticleResponseDTO.AiArticleResultDTO> createAiArticle(
            @Parameter(description = "AI 요약을 생성할 대상 링크 ID (Linku 엔티티의 linkuId). 요청 유저가 저장한 링크여야 합니다.", example = "101") @PathVariable("linkuid") Long linkuId,
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
