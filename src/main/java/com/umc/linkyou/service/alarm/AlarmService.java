package com.umc.linkyou.service.alarm;


import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;

public interface AlarmService {
    void registerFcmToken(Long userId, AlarmRequestDTO.AlarmFcmTokenDTO alarmFcmTokenDTO);
    void deleteFcmToken(Long userId, String fcmToken);
}
