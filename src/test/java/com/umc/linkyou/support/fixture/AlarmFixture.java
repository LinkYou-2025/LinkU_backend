package com.umc.linkyou.support.fixture;

import com.umc.linkyou.domain.*;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.enums.Role;

import java.time.LocalDateTime;

public class AlarmFixture {

    public static final Long USER_ID = 1L;
    public static final Long ALARM_ID = 10L;
    public static final String FCM_TOKEN = "test-fcm-token";

    public static Users user() {
        return Users.builder()
                .id(USER_ID)
                .nickName("테스트유저")
                .role(Role.USER)
                .build();
    }

    public static AlarmSetting defaultSetting(Users user) {
        return AlarmSetting.createDefault(user);
    }

    public static Alarm alarm(AlarmType type) {
        return Alarm.create(type, 1L);
    }

    public static UserAlarm userAlarm(Users user, Alarm alarm) {
        return UserAlarm.create(user, alarm);
    }

    public static UsersFcmToken activeToken(Users user) {
        return UsersFcmToken.builder()
                .user(user)
                .fcmToken(FCM_TOKEN)
                .lastUsedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(60))
                .isActive(true)
                .build();
    }

    public static UsersFcmToken inactiveToken(Users user) {
        return UsersFcmToken.builder()
                .user(user)
                .fcmToken(FCM_TOKEN)
                .lastUsedAt(LocalDateTime.now().minusDays(5))
                .expiresAt(LocalDateTime.now().plusDays(55))
                .isActive(false)
                .build();
    }
}
