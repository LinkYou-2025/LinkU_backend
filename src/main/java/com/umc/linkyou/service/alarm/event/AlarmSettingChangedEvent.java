package com.umc.linkyou.service.alarm.event;

import java.util.List;

import com.umc.linkyou.domain.enums.AlarmSettingType;

public record AlarmSettingChangedEvent(
        Long userId,
        AlarmSettingType alarmSettingType,
        boolean shouldSubscribe,
        List<String> topics) {}
