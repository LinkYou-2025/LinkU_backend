package com.umc.linkyou.web.controller;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.alarm.AlarmSuccessStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.domain.enums.AlarmSettingType;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.enums.Role;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.service.alarm.FcmPushSender;
import com.umc.linkyou.validation.annotation.ApiV1;
import com.umc.linkyou.web.api.AlarmApi;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import com.umc.linkyou.web.dto.alarm.AlarmResponseDTO;
import com.umc.linkyou.web.dto.alarm.AlarmSettingResponseDTO;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@ApiV1
@Validated
@RequiredArgsConstructor
public class AlarmController implements AlarmApi {

    private final AlarmService alarmService;
    private final FcmPushSender fcmPushSender;

    @Override
    public ApiResponse<Object> registerFcmToken(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO
    ) {
        alarmService.registerFcmToken(userDetails.getUserId(), alarmFcmTokenDTO);
        return ApiResponse.onSuccess(AlarmSuccessStatus.FCM_TOKEN_REGISTERED);
    }

    @Override
    public ApiResponse<Object> deleteFcmToken(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO
    ) {
        alarmService.deleteFcmToken(userDetails.getUserId(), alarmFcmTokenDTO.fcmToken());
        return ApiResponse.onSuccess(AlarmSuccessStatus.FCM_TOKEN_DELETED);
    }

    @Override
    public ApiResponse<Object> sendTestAlarm(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.TestAlarmSendDTO request
    ) {
        validateAdmin(userDetails);
        fcmPushSender.sendToToken(
                request.fcmToken(),
                FcmSendRequestDTO.of(AlarmType.ANNOUNCEMENT_UPDATE, userDetails.getUserId())
        );
        return ApiResponse.onSuccess(AlarmSuccessStatus.ALARM_TEST_SENT);
    }

    @Override
    public ApiResponse<AlarmSettingResponseDTO> viewAlarmSetting(
            @CurrentUser CustomUserDetails userDetails
    ) {
        return ApiResponse.onSuccess(AlarmSuccessStatus.ALARM_SETTING_OK, alarmService.viewAlarm(userDetails.getUserId()));
    }

    @Override
    public ApiResponse<Boolean> updateAlarmSetting(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmSettingUpdateDTO request
    ) {
        boolean isEnabled = alarmService.updateNoticeAlarmSetting(userDetails.getUserId(), request.alarmType());
        return ApiResponse.onSuccess(AlarmSuccessStatus.ALARM_SETTING_UPDATED, isEnabled);
    }

    @Override
    public ApiResponse<AlarmResponseDTO.AlarmCursorPageResponse> viewAlarmList(
            @CurrentUser CustomUserDetails userDetails,
            @RequestParam("alarmType") AlarmSettingType alarmType,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.onSuccess(AlarmSuccessStatus.ALARM_LIST_OK, alarmService.viewAlarmList(userDetails.getUserId(), alarmType, cursor, size));
    }

    @Override
    public ApiResponse<AlarmResponseDTO.AlarmDetailDTO> viewAlarmDetail(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long alarmId
    ) {
        return ApiResponse.onSuccess(AlarmSuccessStatus.ALARM_DETAIL_OK, alarmService.viewAlarmDetail(userDetails.getUserId(), alarmId));
    }

    @Override
    public ApiResponse<Object> registerAdminAlarm(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AdminAlarmSendRequestDTO request
    ) {
        validateAdmin(userDetails);
        alarmService.registerAdminAlarm(request);
        return ApiResponse.onSuccess(AlarmSuccessStatus.ALARM_BROADCAST_REGISTERED);
    }

    @Override
    public ApiResponse<Object> markAlarmAsRead(
            @CurrentUser CustomUserDetails userDetails,
            @PathVariable Long alarmId
    ) {
        alarmService.markAlarmAsRead(userDetails.getUserId(), alarmId);
        return ApiResponse.onSuccess(AlarmSuccessStatus.ALARM_READ);
    }

    private void validateAdmin(CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getRole() != Role.ADMIN) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }
    }
}
