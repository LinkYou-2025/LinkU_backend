package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.domain.UsersFcmToken;
import com.umc.linkyou.repository.UserFcmTokenRepository;
import com.umc.linkyou.service.alarm.FcmSubscriber;
import com.umc.linkyou.service.alarm.event.AlarmSettingChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AlarmSettingEventListener {

    private final UserFcmTokenRepository userFcmTokenRepository;
    private final FcmSubscriber fcmSubscriber;

    @Async("fcmTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAlarmSettingChanged(AlarmSettingChangedEvent event) {
        List<UsersFcmToken> tokens =
                userFcmTokenRepository.findAllActiveAndNotExpiredByUserId(event.userId(), LocalDateTime.now());

        for (UsersFcmToken token : tokens) {
            fcmSubscriber.updateTopicSubscription(
                    token.getFcmToken(),
                    event.topics(),
                    event.shouldSubscribe()
            );
        }
    }
}
