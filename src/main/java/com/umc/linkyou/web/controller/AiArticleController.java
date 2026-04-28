package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.domain.AiArticle;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.service.AiArticleService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.AiArticleResponsetDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "ai-article-controller", description = "AI 기사 관련 API")
@ApiV1
@RestController
@RequestMapping("/aiarticle")
@RequiredArgsConstructor
public class AiArticleController {

    final private AiArticleService aiArticleService;
    final private AiArticleRepository aiArticleRepository;
    final private UsersUtils usersUtils;

    @Operation(
            summary = "AI 기사 저장 또는 조회",
            description = "링크 ID에 해당하는 AI 기사 정보를 저장하거나 조회합니다. 이미 존재하면 조회하고, 없으면 생성합니다."
    )
    @PostMapping("/{linkuid}")
    public ApiResponse<AiArticleResponsetDTO.AiArticleResultDTO> saveOrGetAiArticle(
            @PathVariable("linkuid") Long linkuId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);

        AiArticleResponsetDTO.AiArticleResultDTO result =
                aiArticleService.saveOrGetAiArticle(linkuId, userId);

        return ApiResponse.of(SuccessStatus._OK, result); // 상태는 서비스 단에서 조정하지 않고 항상 OK로 반환
    }

    @Operation(
            summary = "마이페이지 카테고리별 AI 요약 링크 조회 (목록)",
            description = "사용자가 저장한 링크 중 AI 요약이 생성된 항목들을 카테고리별로 필터링하여 최신순으로 조회합니다."
    )
    @GetMapping("/category/{categoryId}")
    public ApiResponse<List<LinkuResponseDTO.LinkuResultDTO>> getMyAiArticlesByCategory(
            @Parameter(description = "조회할 카테고리 ID", example = "1")
            @PathVariable("categoryId") Long categoryId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);

        // QueryDSL을 통해 최신순으로 정렬된 목록을 가져옵니다.
        List<LinkuResponseDTO.LinkuResultDTO> result =
                aiArticleService.getMyAiArticlesByCategory(userId, categoryId);

        return ApiResponse.of(SuccessStatus._OK, result);
    }
}
