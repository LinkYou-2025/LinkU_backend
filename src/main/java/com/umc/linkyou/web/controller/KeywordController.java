package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.keyword.KeywordRankResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "keyword-controller", description = "키워드 통계 관련 API")
@ApiV1
@RequiredArgsConstructor
@RequestMapping("/keywords")
public class KeywordController {
    private final KeywordService keywordService;

    @Operation(summary = "내 월별 상위 키워드 3개 조회")
    @GetMapping("/my/top3")
    public ResponseEntity<ApiResponse<List<KeywordRankResponse>>>
    getMyTop3Keywords(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String month
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.onSuccess(keywordService.getMyTop3Keywords(userId, month)));
    }

    @Operation(summary = "같은 직업 유저들의 상위 키워드 15개 조회")
    @GetMapping("/job/top15")
    public ResponseEntity<ApiResponse<List<KeywordRankResponse>>>
    getJobTop15Keywords(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.onSuccess(keywordService.getJobTop15Keywords(userId)));
    }
}
