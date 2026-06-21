package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.domain.AlarmSetting;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.domain.enums.PermissionType;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.service.alarm.event.FolderDeletedAlarmEvent;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.umc.linkyou.support.fixture.AlarmFixture.defaultSetting;
import static com.umc.linkyou.support.fixture.FolderFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FolderDeletedAlarmEventListener 단위 테스트")
class FolderDeletedAlarmEventListenerTest {
    @InjectMocks private FolderDeletedAlarmEventListener listener;

    @Mock private AlarmSettingRepository alarmSettingRepository;
    @Mock private AlarmService alarmService;

    private static final Long MEMBER_ON = 2L;
    private static final Long MEMBER_OFF = 3L;
    private static final Long MEMBER_NO_SETTING = 4L;

    @Nested
    @DisplayName("handle")
    class Handle {
        @Test
        @DisplayName("폴더 알림이 켜진 멤버에게만 sendAlarm을 호출한다")
        void 폴더알림켜진멤버만_발송() {
            Users onUser = participant(MEMBER_ON, PermissionType.VIEWER).getUser();
            Users offUser = participant(MEMBER_OFF, PermissionType.WRITER).getUser();

            AlarmSetting onSetting = defaultSetting(onUser);
            AlarmSetting offSetting = defaultSetting(offUser);
            offSetting.updateFolder(false);

            given(alarmSettingRepository.findByUserId(MEMBER_ON)).willReturn(Optional.of(onSetting));
            given(alarmSettingRepository.findByUserId(MEMBER_OFF)).willReturn(Optional.of(offSetting));
            given(alarmSettingRepository.findByUserId(MEMBER_NO_SETTING)).willReturn(Optional.empty());

            FolderDeletedAlarmEvent event = new FolderDeletedAlarmEvent(
                    FOLDER_ID, List.of(MEMBER_ON, MEMBER_OFF, MEMBER_NO_SETTING), owner().getNickName(), folder().getFolderName());

            listener.handle(event);

            verify(alarmService, times(1)).sendAlarm(eq(MEMBER_ON), any());
            verify(alarmService, never()).sendAlarm(eq(MEMBER_OFF), any());
            verify(alarmService, never()).sendAlarm(eq(MEMBER_NO_SETTING), any());
        }

        @Test
        @DisplayName("sendAlarm 호출 시 닉네임과 폴더명이 values에 정확히 담긴다")
        void sendAlarm_values_정확히전달() {
            Users onUser = participant(MEMBER_ON, PermissionType.VIEWER).getUser();
            given(alarmSettingRepository.findByUserId(MEMBER_ON)).willReturn(Optional.of(defaultSetting(onUser)));

            FolderDeletedAlarmEvent event = new FolderDeletedAlarmEvent(
                    FOLDER_ID, List.of(MEMBER_ON), owner().getNickName(), folder().getFolderName());

            listener.handle(event);

            ArgumentCaptor<AlarmRequestDTO.AlarmSendRequestDTO> captor = ArgumentCaptor.forClass(AlarmRequestDTO.AlarmSendRequestDTO.class);
            verify(alarmService).sendAlarm(eq(MEMBER_ON), captor.capture());

            AlarmRequestDTO.AlarmSendRequestDTO requestDTO = captor.getValue();
            assertThat(requestDTO.type()).isEqualTo(AlarmType.FOLDER_DELETED);
            assertThat(requestDTO.targetId()).isEqualTo(FOLDER_ID);
            assertThat(requestDTO.values()).isEqualTo(Map.of("nickname", owner().getNickName(), "folderName", folder().getFolderName()));
        }
    }
}
