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
    private final Map<String, String> values; // nullable, %s가 있는 알림 시 사용

    private FcmSendRequestDTO(AlarmType type, Long targetId, Map<String, String> values) {
        this.type = type;
        this.targetId = targetId;
        this.values = values;
    }

    // 일반 알림 - title/body 모두 AlarmType에서 자동 설정
    public static FcmSendRequestDTO of(AlarmType type, Long targetId) {
        return new FcmSendRequestDTO(type, targetId, null);
    }

    // 큐레이션 알림 - body의 {placeholder}를 nickname으로 치환
    public static FcmSendRequestDTO withValues(AlarmType type, Long targetId, Map<String, String> values) {
        return new FcmSendRequestDTO(type, targetId, values);
    }

    public String getTitle() {
        return type.getTitle();
    }

    public String getMessage() {
        return AlarmMessageRenderer.render(type.getBody(), values);
    }
}
