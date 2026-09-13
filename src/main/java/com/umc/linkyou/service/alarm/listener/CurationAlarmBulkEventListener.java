package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.service.alarm.FcmPushSender;
import com.umc.linkyou.service.alarm.event.CurationAlarmBulkEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CurationAlarmBulkEventListener {

    private final FcmPushSender fcmPushSender;

    @Async("alarmBatchTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CurationAlarmBulkEvent event) {
        fcmPushSender.sendBulkPersonalized(event.targets());
    }
}
