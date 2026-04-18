package com.umc.linkyou.service.alarm.event;

import com.umc.linkyou.domain.enums.AlarmSettingType;

import java.util.List;

public record AlarmSettingChangedEvent(
        Long userId,
        AlarmSettingType alarmSettingType,
        boolean shouldSubscribe,
        List<String> topics
) {
}