package com.umc.linkyou.web.controller.admin;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.curation.CurationSuccessStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.service.curation.CurationService;
import com.umc.linkyou.validation.annotation.ApiAdmin;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin-curation-controller", description = "큐레이션 관리자 API")
@ApiAdmin
@RestController("CurationAdminController")
@RequiredArgsConstructor
@RequestMapping("/curations")
public class CurationController {

    private final CurationService curationService;

    @Operation(
            summary = "단일 유저 큐레이션 생성",
            description = "userId와 month(YYYY-MM)를 지정해 큐레이션을 즉시 생성합니다.")
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @PostMapping("/batch/manual/user")
    public ResponseEntity<ApiResponse<Object>> triggerBatchForUser(
            @RequestParam Long userId,
            @RequestParam String month
    ) {
        curationService.generateCurationForUser(userId, month);
        return ResponseEntity.ok(ApiResponse.onSuccess(CurationSuccessStatus.CURATION_BATCH_TRIGGERED));
    }
}
