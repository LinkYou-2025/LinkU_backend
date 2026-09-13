package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.CommonErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.jwt.CurrentUser;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.web.dto.tag.MyTagRankResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@Tag(name = "태그 API", description = "태그(감정·상황) 통계 관련 API")
@RequestMapping("/tags")
public interface TagApi {

    @Operation(summary = "내 월별 상위 태그 조회")
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND}, commonErrorStatus = {CommonErrorStatus._BAD_REQUEST})
    @GetMapping("/my")
    ResponseEntity<ApiResponse<List<MyTagRankResponse>>> getMyTopTags(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam YearMonth month,
            @RequestParam(defaultValue = "3") @Min(1) @Max(50) int limit
    );
}
