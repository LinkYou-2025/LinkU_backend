package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.repository.aiArticleRepository.AiArticleRepository;
import com.umc.linkyou.service.AiArticleService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.web.dto.AiArticleResponsetDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/aiarticle")
@RequiredArgsConstructor
public class AiArticleController {

    final private AiArticleService aiArticleService;
    final private AiArticleRepository aiArticleRepository;
    final private UsersUtils usersUtils;

    @PostMapping("/{linkuid}")
    public ApiResponse<AiArticleResponsetDTO.AiArticleResultDTO> saveAiArticle(
            @PathVariable("linkuid") Long linkuId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        boolean exists = aiArticleRepository.existsByLinku_LinkuId(linkuId);
        AiArticleResponsetDTO.AiArticleResultDTO result;
        if (!exists) {
            result = aiArticleService.saveAiArticle(linkuId, userId);
            // 201: 생성! (ApiResponse.of 쓰면 code/message도 생성용으로 가공 가능)
            return ApiResponse.of(SuccessStatus._CREATED, result);
        } else {
            result = aiArticleService.showAiArticle(linkuId, userId);
            // 200: 정상 조회!
            return ApiResponse.of(SuccessStatus._OK, result);
        }
    }


}
