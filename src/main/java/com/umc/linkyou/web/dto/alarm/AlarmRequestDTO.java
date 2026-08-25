package com.umc.linkyou.web.dto.alarm;

import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.enums.AlarmSettingType;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.validation.annotation.ValidEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class AlarmRequestDTO {

    private AlarmRequestDTO() {
    }

    public record AlarmFcmTokenDTO(
            @Schema(description = "클라이언트 앱에서 발급받은 FCM 등록 토큰")
            @NotNull(message = "FcmToken은 필수입니다. ")
            String fcmToken
    ) {
    }

    public record TestAlarmSendDTO(
            @NotBlank(message = "fcmToken은 필수입니다.")
            String fcmToken,
            @NotNull(message = "type은 필수입니다.")
            AlarmType type,
            @NotNull(message = "targetId는 필수입니다.")
            Long targetId
    ) {
    }

    @Schema(description = "알림 설정 수정 요청 DTO")
    public record AlarmSettingUpdateDTO(
            @NotNull(message = "alarmType은 필수입니다.")
            AlarmSettingType alarmType
    ) {
    }

    public record AlarmSendRequestDTO(
            @NotNull(message = "type은 필수입니다.")
            AlarmType type,
            @NotNull(message = "targetId는 필수입니다.")
            Long targetId,
            AlarmPayload payload   // placeholder 치환값. 값 없으면 AlarmPayload.Empty
    ){}

    @Schema(description = "관리자 브로드캐스트 알림 요청 DTO")
    public record AdminAlarmSendRequestDTO(
            @NotNull(message = "type은 필수입니다.")
            @ValidEnum(enumClass = AlarmType.class, anyOf = {"ANNOUNCEMENT_UPDATE", "ANNOUNCEMENT_ERROR"},
                    message = "ANNOUNCEMENT_UPDATE, ANNOUNCEMENT_ERROR 타입만 등록할 수 있습니다.")
            AlarmType type,
            @NotBlank(message = "content는 필수입니다.")
            @Schema(description = "알림 상세 내용")
            String content
    ){}

}
