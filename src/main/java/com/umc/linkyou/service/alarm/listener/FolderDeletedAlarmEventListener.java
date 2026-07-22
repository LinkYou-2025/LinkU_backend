package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.service.alarm.AlarmService;
import com.umc.linkyou.service.alarm.event.FolderDeletedAlarmEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class FolderDeletedAlarmEventListener {
    private final AlarmService alarmService;

    @Async("alarmBatchTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FolderDeletedAlarmEvent event) {
        AlarmPayload payload = new AlarmPayload.NicknameAndFolder(
                event.deleterNickname(), event.folderName());

        // 멤버 전원에게 동일 본문으로 일괄 발송. 설정 필터링은 sendAlarmBulk 내부에서 처리한다.
        alarmService.sendAlarmBulk(
                event.memberIds(), AlarmType.FOLDER_DELETED, event.folderId(), payload);
    }
}
