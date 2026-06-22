package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.repository.AlarmSettingRepository;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.service.alarm.event.FolderDeletedAlarmEvent;
import com.umc.linkyou.web.dto.alarm.AlarmRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.umc.linkyou.domain.AlarmSetting;

import java.util.Map;

@Slf4j
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

        alarmSettingRepository.findAllByUserIdIn(event.memberIds()).stream()
                .filter(AlarmSetting::isFolderActive)
                .forEach(setting -> {
                    try {
                        alarmService.sendAlarm(setting.getUser().getId(),
                                new AlarmRequestDTO.AlarmSendRequestDTO(AlarmType.FOLDER_DELETED, event.folderId(), values));
                    } catch (Exception e) {
                        log.error("폴더 삭제 알림 발송 실패. userId={}", setting.getUser().getId(), e);
                    }
                });
    }
}