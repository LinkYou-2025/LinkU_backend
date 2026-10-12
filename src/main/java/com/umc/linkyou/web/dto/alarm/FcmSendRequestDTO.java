package com.umc.linkyou.web.dto.alarm;

import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.service.alarm.AlarmMessageRenderer;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class FcmSendRequestDTO {

    private final AlarmType type;
    private final Long targetId;
    private final Long alarmId; // nullable, 저장된 Alarm 엔티티의 PK
    private final Map<String, String> values; // nullable, %s가 있는 알림 시 사용

    private FcmSendRequestDTO(AlarmType type, Long targetId, Long alarmId, Map<String, String> values) {
        this.type = type;
        this.targetId = targetId;
        this.alarmId = alarmId;
        this.values = values;
    }

    // 저장되지 않는 알림 - title/body 모두 AlarmType에서 자동 설정, alarmId 없음
    public static FcmSendRequestDTO of(AlarmType type, Long targetId) {
        return new FcmSendRequestDTO(type, targetId, null, null);
    }

    // 개인 알림 - 저장된 Alarm의 PK를 함께 전달
    public static FcmSendRequestDTO of(AlarmType type, Long targetId, Long alarmId) {
        return new FcmSendRequestDTO(type, targetId, alarmId, null);
    }

    // 큐레이션 알림 - body의 {placeholder}를 nickname으로 치환
    public static FcmSendRequestDTO withValues(AlarmType type, Long targetId, Long alarmId, Map<String, String> values) {
        return new FcmSendRequestDTO(type, targetId, alarmId, values);
    }

    public String getTitle() {
        return type.getTitle();
    }

    public String getMessage() {
        return AlarmMessageRenderer.render(type.getBody(), values);
    }
}
