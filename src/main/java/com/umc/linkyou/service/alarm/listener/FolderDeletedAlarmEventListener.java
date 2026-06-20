package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.service.alarm.event.FolderDeletedAlarmEvent;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.umc.linkyou.domain.AlarmSetting;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class FolderDeletedAlarmEventListener {
    private final AlarmSettingRepository alarmSettingRepository;
    private final AlarmService alarmService;

    @Async("alarmBatchTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FolderDeletedAlarmEvent event) {
        Map<String, String> values = Map.of(
                "nickname", event.deleterNickname(),
                "folderName", event.folderName()
        );

        event.memberIds().forEach(memberId -> alarmSettingRepository.findByUserId(memberId)
                .filter(AlarmSetting::isFolderActive)
                .ifPresent(setting -> alarmService.sendAlarm(memberId, new AlarmRequestDTO.AlarmSendRequestDTO(AlarmType.FOLDER_DELETED, event.folderId(), values))));
    }
}