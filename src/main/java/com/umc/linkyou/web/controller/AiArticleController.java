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
            summary = "마이페이지 카테고리별 AI 요약 링크 조회 (목록, 페이징)",
            description = "커서 기반 페이징을 지원합니다. 첫 조회 시 cursor는 생략 가능합니다."
    )
    @GetMapping("/category/{categoryId}")
    public ApiResponse<LinkuResponseDTO.LinkuSliceResultDTO> getMyAiArticlesByCategory(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(name = "cursor", required = false) Long cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);

        LinkuResponseDTO.LinkuSliceResultDTO result =
                aiArticleService.getMyAiArticlesByCategory(userId, categoryId, cursor, limit);

        return ApiResponse.of(SuccessStatus._OK, result);
    }
}
