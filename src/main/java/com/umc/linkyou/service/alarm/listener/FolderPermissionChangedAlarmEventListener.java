package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.service.alarm.event.FolderPermissionChangedAlarmEvent;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class FolderPermissionChangedAlarmEventListener {
    private final AlarmService alarmService;

    @Async("alarmBatchTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FolderPermissionChangedAlarmEvent event) {
        alarmService.sendAlarm(event.memberId(), new AlarmRequestDTO.AlarmSendRequestDTO(
                AlarmType.FOLDER_PERMISSION_CHANGED, event.folderId(), Map.of("folderName", event.folderName())));
    }
}
