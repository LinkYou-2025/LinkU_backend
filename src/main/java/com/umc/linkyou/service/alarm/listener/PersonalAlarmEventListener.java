package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.service.alarm.FcmPushSender;
import com.umc.linkyou.service.alarm.event.PersonalAlarmEvent;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PersonalAlarmEventListener {

    private final FcmPushSender fcmPushSender;

    @Async("fcmTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(PersonalAlarmEvent event) {
        Map<String, String> values = event.payload().toValues();
        FcmSendRequestDTO requestDTO = values.isEmpty()
                ? FcmSendRequestDTO.of(event.alarmType(), event.targetId()) // data 없을 때
                : FcmSendRequestDTO.withValues(event.alarmType(), event.targetId(), values); // data 있을 때

        fcmPushSender.sendToUser(event.userId(), requestDTO);
    }
}
