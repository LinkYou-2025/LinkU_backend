package com.umc.linkyou.web.api;

import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.domain.enums.AlarmSettingType;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import com.umc.linkyou.web.dto.alarm.AlarmResponseDTO;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    @Operation(
            summary = "FCM 토큰 등록",
            description = """
                    FCM 토큰을 등록합니다.
                    - 요청 body의 `fcmToken`은 필수값입니다.
                    - 등록된 토큰은 이후 푸시 알림 전송에 사용됩니다.
                    - 인증되지 않은 사용자는 인증 에러를 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FCM 토큰 등록 성공"),
    })
    @PostMapping("/fcmtoken")
    ApiResponse<String> registerFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO
    );

    @Operation(
            summary = "FCM 토큰 삭제",
            description = """
                    등록된 FCM 토큰을 삭제합니다.
                    - 요청 body의 `fcmToken`은 필수값입니다.
                    - 삭제된 토큰으로는 더 이상 푸시 알림을 수신하지 않습니다.
                    - 인증되지 않은 사용자는 인증 에러를 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FCM 토큰 삭제 성공"),
    })
    @DeleteMapping("/fcmtoken")
    ApiResponse<String> deleteFcmToken(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO
    );

    @Operation(
            summary = "테스트 알림 전송",
            description = """
                    발급받은 FCM 토큰으로 테스트 알림을 전송합니다.
                    - `fcmToken`은 필수값입니다.
                    - 관리자 권한으로만 호출할 수 있습니다.
                    - 관리자 권한이 없으면 접근 권한 예외를 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 알림 전송 성공")
    })
    @PostMapping("/test/send")
    ApiResponse<String> sendTestAlarm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.TestAlarmSendDTO request
    );

    @Operation(
            summary = "알림 설정 조회",
            description = """
                    사용자의 알림 설정 상태를 조회합니다.
                    - 전체 알림, 폴더 알림, 링크 알림, 큐레이션 알림, 공지 알림 상태를 반환합니다.
                    - 인증된 사용자만 조회할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 조회 성공")
    })
    @GetMapping("/settings")
    ApiResponse<?> viewAlarmSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails
    );

    @Operation(
            summary = "알림 설정 수정",
            description = """
                    선택한 알림 타입의 상태를 변경합니다.
                    - 이미 off이면 on으로, on이면 off로 변경됩니다.
                    - body에 아래 타입 중 하나를 필수로 포함해야 합니다.
                    - `ALL` : 모든 알림
                    - `FOLDER` : 폴더 알림
                    - `LINK` : 링크 알림
                    - `CURATION` : 큐레이션 알림
                    - `NOTICE` : 공지 알림
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 설정 수정 성공")
    })
    @PatchMapping("/settings")
    ApiResponse<Boolean> updateAlarmSetting(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AlarmSettingUpdateDTO request
    );

    @Operation(
            summary = "알림 목록 조회",
            description = """
                    알림 목록을 커서 기반으로 조회합니다.
                    - `alarmType`은 조회할 알림 타입이며 필수값입니다.
                    - 허용값은 `ALL`, `FOLDER`, `LINK`, `CURATION`, `NOTICE` 입니다.
                    - `cursor`가 null이면 최신 알림부터 조회합니다.
                    - `size`의 기본값은 20입니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 목록 조회 성공")
    })
    @GetMapping("/list")
    ApiResponse<AlarmResponseDTO.AlarmCursorPageResponse> viewAlarmList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "알림 설정 타입 (ALL/FOLDER/LINK/CURATION/NOTICE)")
            @RequestParam("alarmType") AlarmSettingType alarmType,
            @Parameter(description = "커서(userAlarmId). null일 경우 최신부터 조회")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "조회 개수")
            @RequestParam(defaultValue = "20") int size
    );

    @Operation(
            summary = "알림 상세 조회",
            description = """
                    특정 알림의 상세 정보를 조회합니다.
                    - `alarmId`에 해당하는 알림 상세 데이터를 반환합니다.
                    - 본인 알림이 아니거나 존재하지 않는 알림이면 예외를 반환할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 상세 조회 성공")
    })
    @GetMapping("/detail/{alarmId}")
    ApiResponse<AlarmResponseDTO.AlarmDetailDTO> viewAlarmDetail(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "조회할 알림 ID")
            @PathVariable Long alarmId
    );

    @Operation(
            summary = "관리자 브로드캐스트 알림 발송",
            description = """
                    관리자 권한으로 브로드캐스트 알림을 등록하고 토픽 발송 이벤트를 발생시킵니다.
                    - 관리자만 호출할 수 있습니다.
                    - 관리자 권한이 없으면 접근 권한 예외를 반환합니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관리자 브로드캐스트 알림 발송 성공")
    })
    @PostMapping("/admin/broadcast")
    ApiResponse<String> registerAdminAlarm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlarmRequestDTO.AdminAlarmSendRequestDTO request
    );

    @Operation(
            summary = "알림 읽음 처리",
            description = """
                    특정 알림을 읽음 상태로 변경합니다.
                    - `alarmId`에 해당하는 알림을 읽음 처리합니다.
                    - 본인 알림이 아니거나 존재하지 않는 알림이면 예외를 반환할 수 있습니다.
                    """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공")
    })
    @PatchMapping("/{alarmId}/read")
    ApiResponse<String> markAlarmAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "읽음 처리할 알림 ID")
            @PathVariable Long alarmId
    );
}
