package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.domain.enums.AlarmSettingType;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.service.alarm.FcmPushSender;
import com.umc.linkyou.utils.UsersUtils;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.AlarmApi;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import com.umc.linkyou.web.dto.alarm.AlarmResponseDTO;
import com.umc.linkyou.web.dto.alarm.AlarmSettingResponseDTO;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@ApiV1
@RequiredArgsConstructor
public class AlarmController implements AlarmApi {

    private final AlarmService alarmService;
    private final FcmPushSender fcmPushSender;
    private final UsersUtils usersUtils;

    @Override
    public ApiResponse<String> registerFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        alarmService.registerFcmToken(userId, alarmFcmTokenDTO);
        return ApiResponse.onSuccess("FCM 토큰이 정상적으로 등록되었습니다.");
    }

    @Override
    public ApiResponse<String> deleteFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        alarmService.deleteFcmToken(userId, alarmFcmTokenDTO.fcmToken());
        return ApiResponse.onSuccess("FCM 토큰이 정상적으로 삭제되었습니다.");
    }

    @Override
    public ApiResponse<String> sendTestAlarm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.TestAlarmSendDTO request
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        fcmPushSender.sendToToken(
                request.fcmToken(),
                FcmSendRequestDTO.of(AlarmType.ANNOUNCEMENT_UPDATE, userId)
        );
        return ApiResponse.onSuccess("테스트 알림이 정상적으로 전송되었습니다.");
    }

    // 알림 설정 조회
    @Override
    public ApiResponse<AlarmSettingResponseDTO> viewAlarmSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        return ApiResponse.onSuccess(alarmService.viewAlarm(userId));
    }

    @Override
    public ApiResponse<Boolean> updateAlarmSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmSettingUpdateDTO request
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        boolean isEnabled = alarmService.updateNoticeAlarmSetting(userId, request.alarmType());
        return ApiResponse.onSuccess("알림 설정이 정상적으로 수정되었습니다.", isEnabled);
    }

    @Override
    public ApiResponse<AlarmResponseDTO.AlarmCursorPageResponse> viewAlarmList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("alarmType") AlarmSettingType alarmType,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        return ApiResponse.onSuccess(alarmService.viewAlarmList(userId, alarmType, cursor, size));
    }

    @Override
    public ApiResponse<AlarmResponseDTO.AlarmDetailDTO> viewAlarmDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long alarmId
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        return ApiResponse.onSuccess(alarmService.viewAlarmDetail(userId, alarmId));
    }

    @Override
    public ApiResponse<String> registerAdminAlarm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AdminAlarmSendRequestDTO request
    ) {
        validateAdmin(userDetails);
        alarmService.registerAdminAlarm(request);
        return ApiResponse.onSuccess("관리자 브로드캐스트 알림이 정상적으로 등록되었습니다.");
    }

    @Override
    public ApiResponse<String> markAlarmAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long alarmId
    ) {
        Long userId = usersUtils.getAuthenticatedUserId(userDetails);
        alarmService.markAlarmAsRead(userId, alarmId);
        return ApiResponse.onSuccess("알림이 읽음 처리되었습니다.");
    }

    private void validateAdmin(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getUsers() == null || userDetails.getUsers().getRole() != Role.ADMIN) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }
    }


}
