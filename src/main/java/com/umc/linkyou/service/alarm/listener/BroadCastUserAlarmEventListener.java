package com.umc.linkyou.service.alarm.listener;

import com.umc.linkyou.apiPayload.code.status.alarm.AlarmErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Alarm;
import com.umc.linkyou.domain.UserAlarm;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.AlarmRepository;
import com.umc.linkyou.repository.UserAlarmRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.alarm.event.BroadCastUserAlarmCreateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BroadCastUserAlarmEventListener {

    private static final int PAGE_SIZE = 1000;

    private final AlarmRepository alarmRepository;
    private final UserRepository userRepository;
    private final UserAlarmRepository userAlarmRepository;

    @Async("alarmTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(BroadCastUserAlarmCreateEvent event) {
        Alarm alarm = alarmRepository.findById(event.alarmId())
                .orElseThrow(() -> new GeneralException(AlarmErrorStatus.ALARM_NOT_FOUND));

        int page = 0;
        Slice<Users> slice;

        do {
            slice = userRepository.findAll(PageRequest.of(page, PAGE_SIZE));
            List<UserAlarm> batch = slice.getContent().stream()
                    .map(user -> UserAlarm.create(user, alarm))
                    .toList();
            if (!batch.isEmpty()) {
                userAlarmRepository.saveAll(batch);
            }
            page++;
        } while (slice.hasNext());
    }
}
