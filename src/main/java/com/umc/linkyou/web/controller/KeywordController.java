package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.dto.keyword.KeywordRankResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "keyword-controller", description = "키워드 통계 관련 API")
@ApiV1
@Validated
@RequiredArgsConstructor
@RequestMapping("/keywords")
public class KeywordController {

    private final KeywordService keywordService;

    @Operation(summary = "내 월별 상위 키워드 조회")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<KeywordRankResponse>>> getMyTopKeywords(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String month,
            @RequestParam(defaultValue = "3") @Min(1) @Max(50) int limit
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.onSuccess(keywordService.getMyTopKeywords(userId, month, limit)));
    }

    @Operation(summary = "같은 직업 유저들의 상위 키워드 조회")
    @GetMapping("/job")
    public ResponseEntity<ApiResponse<List<KeywordRankResponse>>> getJobTopKeywords(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam(defaultValue = "15") @Min(1) @Max(50) int limit
    ) {
        Long userId = userDetails.getUserId();
        return ResponseEntity.ok(ApiResponse.onSuccess(keywordService.getJobTopKeywords(userId, limit)));
    }
}
