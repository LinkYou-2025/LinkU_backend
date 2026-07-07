package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.service.tag.TagService;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.TagApi;
import com.umc.linkyou.web.dto.tag.MyTagRankResponse;
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
public class TagController implements TagApi {

    private final TagService tagService;

    @Override
    public ResponseEntity<ApiResponse<List<MyTagRankResponse>>> getMyTopTags(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam String month,
            @RequestParam(defaultValue = "3") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(ApiResponse.onSuccess(tagService.getMyTopTags(userDetails.getUserId(), month, limit)));
    }
}
