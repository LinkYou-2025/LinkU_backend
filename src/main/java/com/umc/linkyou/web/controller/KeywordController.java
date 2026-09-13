package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.KeywordApi;
import com.umc.linkyou.web.dto.keyword.JobKeywordRankResponse;
import com.umc.linkyou.web.dto.keyword.KeywordLinkuItemDTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@ApiV1
@Validated
@RequiredArgsConstructor
public class KeywordController implements KeywordApi {

    private final KeywordService keywordService;

    @Override
    public ResponseEntity<ApiResponse<List<JobKeywordRankResponse>>> getJobTopKeywords(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam YearMonth month,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(ApiResponse.onSuccess(keywordService.getJobTopKeywords(userDetails.getUserId(), month, limit)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<KeywordLinkuItemDTO>>> getLinkusByKeyword(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable String keyword) {
        return ResponseEntity.ok(ApiResponse.onSuccess(keywordService.getLinkusByKeyword(userDetails.getUserId(), keyword)));
    }
}
