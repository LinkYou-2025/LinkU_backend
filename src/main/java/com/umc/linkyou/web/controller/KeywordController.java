package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.keyword.KeywordService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.KeywordApi;
import com.umc.linkyou.web.dto.keyword.KeywordRankResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@ApiV1
@Validated
@RequiredArgsConstructor
public class KeywordController implements KeywordApi {

    private final KeywordService keywordService;

    @Override
    public ResponseEntity<ApiResponse<List<KeywordRankResponse>>> getMyTopKeywords(@CurrentUser CustomUserDetails userDetails, @RequestParam String month, @RequestParam(defaultValue = "3") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(ApiResponse.onSuccess(keywordService.getMyTopKeywords(userDetails.getUserId(), month, limit)));
    }

    @Override
    public ResponseEntity<ApiResponse<List<KeywordRankResponse>>> getJobTopKeywords(@CurrentUser CustomUserDetails userDetails, @RequestParam(defaultValue = "15") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(ApiResponse.onSuccess(keywordService.getJobTopKeywords(userDetails.getUserId(), limit)));
    }
}
