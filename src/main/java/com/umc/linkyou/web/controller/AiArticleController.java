package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.service.AiArticleService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.AiArticleApi;
import com.umc.linkyou.web.dto.AiArticleResponsetDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@ApiV1
@RestController
@RequestMapping("/aiarticle")
@RequiredArgsConstructor
public class AiArticleController implements AiArticleApi {

    private final AiArticleService aiArticleService;
    private final UsersUtils usersUtils;

    @Override
    public ApiResponse<AiArticleResponsetDTO.AiArticleResultDTO> saveOrGetAiArticle(
            @PathVariable("linkuid") Long linkuId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        AiArticleResponsetDTO.AiArticleResultDTO result = aiArticleService.saveOrGetAiArticle(linkuId, userId);
        return ApiResponse.of(SuccessStatus._OK, result);
    }

    @Override
    public ApiResponse<LinkuResponseDTO.LinkuSliceResultDTO> getMyAiArticlesByCategory(
            @PathVariable("categoryId") Long categoryId,
            @RequestParam(name = "cursor", required = false) Long cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        LinkuResponseDTO.LinkuSliceResultDTO result = aiArticleService.getMyAiArticlesByCategory(userId, categoryId, cursor, limit);
        return ApiResponse.of(SuccessStatus._OK, result);
    }
}
