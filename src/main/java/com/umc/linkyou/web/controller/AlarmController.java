package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.converter.LinkuConverter;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.service.category.CategoryService;
import com.umc.linkyou.web.dto.UserRequestDTO;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import com.umc.linkyou.web.dto.alarm.AlarmResponseDTO;
import com.umc.linkyou.web.dto.category.CategoryListResponseDTO;
import com.umc.linkyou.web.dto.linku.LinkuRequestDTO;
import com.umc.linkyou.web.dto.linku.LinkuResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alarm")
public class AlarmController {

    private final AlarmService alarmService;

    // FCM 토큰 보내기
    @PostMapping("/fcmtoken")
    @Operation(summary = "FCM 토큰 등록")
    public ApiResponse<String> registerFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO
    ) {
        if (userDetails == null) {
            return ApiResponse.onFailure(
                    ErrorStatus._INVALID_TOKEN.getCode(),
                    ErrorStatus._INVALID_TOKEN.getMessage(),
                    null
            );
        }
        Long userId = userDetails.getUsers().getId();
        alarmService.registerFcmToken(userId, alarmFcmTokenDTO); //정상 등록되면
        return ApiResponse.onSuccess("FCM 토큰이 정상적으로 등록되었습니다.");
    }

}
