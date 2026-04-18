package com.umc.linkyou.service.alarm.event;

import com.umc.linkyou.domain.enums.AlarmType;

public record BroadCastAlarmEvent(
        AlarmType alarmType,
        Long targetId
) {}
