package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.keyword.JobKeywordRankResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "키워드 API", description = "링크 키워드 통계 관련 API")
@RequestMapping("/keywords")
public interface KeywordApi {

    @Operation(summary = "같은 직업 유저들이 해당 월에 저장한 링크의 상위 키워드 조회")
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND, UserErrorStatus._JOB_NOT_SET})
    @GetMapping("/job")
    ResponseEntity<ApiResponse<List<JobKeywordRankResponse>>> getJobTopKeywords(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam YearMonth month,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    );
}
