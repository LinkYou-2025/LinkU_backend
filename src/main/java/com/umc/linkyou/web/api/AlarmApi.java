package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import com.umc.linkyou.apiPayload.code.status.alarm.AlarmErrorStatus;
import com.umc.linkyou.domain.enums.AlarmSettingType;
import com.umc.linkyou.jwt.CustomUserDetails;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.validation.annotation.swagger.ApiErrorCode;
import com.umc.linkyou.validation.annotation.swagger.ApiSuccessCode;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import com.umc.linkyou.web.dto.alarm.AlarmResponseDTO;
import com.umc.linkyou.web.dto.alarm.AlarmSettingResponseDTO;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.umc.linkyou.jwt.CurrentUser;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "알림 API", description = "알림 관련 API")
@RequestMapping("/alarm")
public interface AlarmApi {

    @Operation(summary = "FCM 토큰 등록", description = """
            FCM 토큰을 등록합니다.
            - 요청 body의 `fcmToken`은 필수값입니다.
            - 등록된 토큰은 이후 푸시 알림 전송에 사용됩니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @PostMapping("/fcmtoken")
    ApiResponse<Void> registerFcmToken(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO
    );

    @Operation(summary = "FCM 토큰 삭제", description = """
            등록된 FCM 토큰을 삭제합니다.
            - 요청 body의 `fcmToken`은 필수값입니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @DeleteMapping("/fcmtoken")
    ApiResponse<Void> deleteFcmToken(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO
    );

    @Operation(summary = "테스트 알림 전송", description = """
            발급받은 FCM 토큰으로 테스트 알림을 전송합니다.
            - `fcmToken`은 필수값입니다.
            - 관리자 권한으로만 호출할 수 있습니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(errorStatus = {ErrorStatus._FORBIDDEN})
    @ApiErrorCode(alarmErrorStatus = {AlarmErrorStatus.ALARM_SEND_FAILED})
    @PostMapping("/test/send")
    ApiResponse<Void> sendTestAlarm(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.TestAlarmSendDTO request
    );

    @Operation(summary = "알림 설정 조회", description = """
            사용자의 알림 설정 상태를 조회합니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @ApiErrorCode(alarmErrorStatus = {AlarmErrorStatus.ALARM_NOT_FOUND})
    @GetMapping("/settings")
    ApiResponse<AlarmSettingResponseDTO> viewAlarmSetting(
            @CurrentUser CustomUserDetails userDetails
    );

    @Operation(summary = "알림 설정 수정", description = """
            선택한 알림 타입의 상태를 변경합니다.
            - 이미 off이면 on으로, on이면 off로 변경됩니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @ApiErrorCode(alarmErrorStatus = {AlarmErrorStatus.ALARM_NOT_FOUND, AlarmErrorStatus.ALARM_TOPIC_SUBSCRIPTION_FAILED})
    @PatchMapping("/settings")
    ApiResponse<Boolean> updateAlarmSetting(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmSettingUpdateDTO request
    );

    @Operation(summary = "알림 목록 조회", description = """
            알림 목록을 커서 기반으로 조회합니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(errorStatus = {ErrorStatus._BAD_REQUEST})
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @GetMapping("/list")
    ApiResponse<AlarmResponseDTO.AlarmCursorPageResponse> viewAlarmList(
            @CurrentUser CustomUserDetails userDetails,
            @Parameter(description = "알림 설정 타입 (ALL/FOLDER/LINK/CURATION/NOTICE)")
            @RequestParam("alarmType") AlarmSettingType alarmType,
            @Parameter(description = "커서(userAlarmId). null일 경우 최신부터 조회")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(summary = "알림 상세 조회", description = """
            특정 알림의 상세 정보를 조회합니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(alarmErrorStatus = {AlarmErrorStatus.ALARM_NOT_FOUND})
    @GetMapping("/detail/{alarmId}")
    ApiResponse<AlarmResponseDTO.AlarmDetailDTO> viewAlarmDetail(
            @CurrentUser CustomUserDetails userDetails,
            @Parameter(description = "조회할 알림 ID")
            @PathVariable Long alarmId
    );

    @Operation(summary = "관리자 브로드캐스트 알림 발송", description = """
            관리자 권한으로 브로드캐스트 알림을 등록합니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(errorStatus = {ErrorStatus._FORBIDDEN})
    @ApiErrorCode(alarmErrorStatus = {AlarmErrorStatus.ALARM_TOPIC_SUBSCRIPTION_FAILED})
    @PostMapping("/admin/broadcast")
    ApiResponse<Void> registerAdminAlarm(
            @CurrentUser CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AdminAlarmSendRequestDTO request
    );

    @Operation(summary = "알림 읽음 처리", description = """
            특정 알림을 읽음 상태로 변경합니다.
            """)
    @ApiSuccessCode(SuccessStatus._OK)
    @ApiErrorCode(userErrorStatus = {UserErrorStatus._USER_NOT_FOUND})
    @ApiErrorCode(alarmErrorStatus = {AlarmErrorStatus.ALARM_NOT_FOUND})
    @PatchMapping("/{alarmId}/read")
    ApiResponse<Void> markAlarmAsRead(
            @CurrentUser CustomUserDetails userDetails,
            @Parameter(description = "읽음 처리할 알림 ID")
            @PathVariable Long alarmId
    );
}
