package com.umc.linkyou.service.alarm.listener;

import com.google.firebase.messaging.FirebaseMessagingException;
import com.umc.linkyou.service.alarm.FcmPushSender;
import com.umc.linkyou.service.alarm.event.BroadCastAlarmEvent;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class BroadCastAlarmEventListener {

    private final FcmPushSender fcmPushSender;

    @Async("fcmTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(BroadCastAlarmEvent event) throws FirebaseMessagingException {
        fcmPushSender.sendToTopic(
                FcmSendRequestDTO.of(event.alarmType(), event.targetId())
        );
    }
}
