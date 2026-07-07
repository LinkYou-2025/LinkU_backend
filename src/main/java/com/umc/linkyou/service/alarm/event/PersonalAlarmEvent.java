package com.umc.linkyou.service.alarm.event;

import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.enums.AlarmType;

public record PersonalAlarmEvent(
        Long userId,
        AlarmType alarmType,
        Long targetId,
        AlarmPayload payload   // placeholder 치환값. 값 없는 알림은 AlarmPayload.Empty
) {
    // placeholder 없는 경우
    public static PersonalAlarmEvent of(Long userId, AlarmType alarmType, Long targetId) {
        return new PersonalAlarmEvent(userId, alarmType, targetId, new AlarmPayload.Empty());
    }

    // placeholder 있는 경우
    public static PersonalAlarmEvent withPayload(Long userId, AlarmType alarmType, Long targetId, AlarmPayload payload) {
        return new PersonalAlarmEvent(userId, alarmType, targetId, payload);
    }
}
