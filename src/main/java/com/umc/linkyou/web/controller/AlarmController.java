package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alarm")
public class AlarmController {

    private final AlarmService alarmService;
    private final UsersUtils usersUtils;

    // FCM 토큰 보내기
    @PostMapping("/fcmtoken")
    @Operation(summary = "FCM 토큰 등록")
    public ApiResponse<String> registerFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        alarmService.registerFcmToken(userId, alarmFcmTokenDTO); //정상 등록되면
        return ApiResponse.onSuccess("FCM 토큰이 정상적으로 등록되었습니다.");
    }

}
