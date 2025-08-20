package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.domain.AiArticle;
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
    public ApiResponse<AiArticleResponsetDTO.AiArticleResultDTO> saveOrGetAiArticle(
            @PathVariable("linkuid") Long linkuId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);

        AiArticleResponsetDTO.AiArticleResultDTO result =
                aiArticleService.saveOrGetAiArticle(linkuId, userId);

        return ApiResponse.of(SuccessStatus._OK, result); // 상태는 서비스 단에서 조정하지 않고 항상 OK로 반환
    }


}
