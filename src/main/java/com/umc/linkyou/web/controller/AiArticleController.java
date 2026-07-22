package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleSuccessStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.service.AiArticleService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.AiArticleApi;
import com.umc.linkyou.web.dto.AiArticleResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@ApiV1
@RestController
@RequestMapping("/aiarticle")
@RequiredArgsConstructor
public class AiArticleController implements AiArticleApi {

    final private AiArticleService aiArticleService;
    final private AiArticleRepository aiArticleRepository;

    @Override
    @PostMapping("/{linkuid}")
    public ApiResponse<AiArticleResponseDTO.AiArticleResultDTO> saveOrGetAiArticle(
            @PathVariable("linkuid") Long linkuId,
            @CurrentUser CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();

        AiArticleResponseDTO.AiArticleResultDTO result =
                aiArticleService.saveOrGetAiArticle(linkuId, userId);
        return ApiResponse.onSuccess(AiArticleSuccessStatus.AI_ARTICLE_OK, result);
    }

    @Override
    @GetMapping
    public ApiResponse<LinkuResponseDTO.LinkuSliceResultDTO> getMyAiArticlesByCategory(
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(name = "cursor", required = false) Long cursor,
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @CurrentUser CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        LinkuResponseDTO.LinkuSliceResultDTO result = aiArticleService.getMyAiArticlesByCategory(userId, categoryId, cursor, limit);
        return ApiResponse.onSuccess(AiArticleSuccessStatus.AI_ARTICLE_LIST_OK, result);
    }
}
