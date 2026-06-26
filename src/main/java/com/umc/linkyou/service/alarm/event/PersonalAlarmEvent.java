package com.umc.linkyou.service.alarm.event;

import com.umc.linkyou.domain.enums.AlarmType;

import java.util.List;
import java.util.Map;

public record PersonalAlarmEvent(
        Long userId,
        AlarmType alarmType,
        Long targetId,
        Map<String, String> values  // nullable, {placeholder}가 있는 알림 시에만 사용
) {
    // nickname 없는 경우 (LINK, FOLDER 등)
    public static PersonalAlarmEvent of(Long userId, AlarmType alarmType, Long targetId) {
        return new PersonalAlarmEvent(userId, alarmType, targetId, null);
    }

    // nickname 있는 경우 (CURATION)
    public static PersonalAlarmEvent withValues(Long userId, AlarmType alarmType, Long targetId, Map<String, String> values) {
        return new PersonalAlarmEvent(userId, alarmType, targetId, values);
    }
}
