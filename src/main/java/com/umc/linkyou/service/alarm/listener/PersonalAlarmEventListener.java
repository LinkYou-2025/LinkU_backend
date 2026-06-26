package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.service.alarm.FcmPushSender;
import com.umc.linkyou.service.alarm.event.PersonalAlarmEvent;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PersonalAlarmEventListener {

    private final FcmPushSender fcmPushSender;

    @Async("fcmTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PersonalAlarmEvent event) {
        FcmSendRequestDTO requestDTO = (event.values() != null && !event.values().isEmpty())
                ? FcmSendRequestDTO.withValues(event.alarmType(), event.targetId(), event.values())
                : FcmSendRequestDTO.of(event.alarmType(), event.targetId());

        fcmPushSender.sendToUser(event.userId(), requestDTO);
    }
}
