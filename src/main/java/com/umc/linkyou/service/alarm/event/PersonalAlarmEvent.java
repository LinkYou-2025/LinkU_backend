package com.umc.linkyou.service.alarm.event;

import com.umc.linkyou.domain.enums.AlarmType;

public record PersonalAlarmEvent(
        Long userId,
        AlarmType alarmType,
        Long targetId,
        String nickname  // nullable, CURATION 알림 시에만 사용
) {
    // nickname 없는 경우 (LINK, FOLDER 등)
    public static PersonalAlarmEvent of(Long userId, AlarmType alarmType, Long targetId) {
        return new PersonalAlarmEvent(userId, alarmType, targetId, null);
    }

    // nickname 있는 경우 (CURATION)
    public static PersonalAlarmEvent ofWithNickname(Long userId, AlarmType alarmType, Long targetId, String nickname) {
        return new PersonalAlarmEvent(userId, alarmType, targetId, nickname);
    }
}
