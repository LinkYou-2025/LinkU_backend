package com.umc.linkyou.service.curation;

import com.umc.linkyou.batch.curation.CurationAlarmBatchItem;
import com.umc.linkyou.domain.Alarm;
import com.umc.linkyou.domain.AlarmPayload;
import com.umc.linkyou.domain.UserAlarm;
import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.repository.AlarmRepository;
import com.umc.linkyou.repository.UserAlarmRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.service.alarm.AlarmMessageRenderer;
import com.umc.linkyou.service.alarm.event.CurationAlarmBulkEvent;
import com.umc.linkyou.web.dto.alarm.FcmBulkTarget;
import com.umc.linkyou.web.dto.alarm.FcmSendRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 큐레이션 알림 발송 배치 전용 서비스
 *
 * <p>writer가 넘긴 청크(최대 100건)를 Alarm/UserAlarm으로 저장하고,
 * 커밋 이후 유저별 개인화 본문을 유지한 채 FCM 발송을 청크당 1회로 묶어 트리거한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CurationAlarmBatchService {

    private final AlarmRepository alarmRepository;
    private final UserAlarmRepository userAlarmRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void sendCurationAlarms(List<? extends CurationAlarmBatchItem> items) {
        if (items.isEmpty()) {
            return;
        }

        List<FcmBulkTarget> targets = new ArrayList<>();
        for (CurationAlarmBatchItem item : items) {
            AlarmPayload payload = new AlarmPayload.Nickname(item.nickname());
            String renderedBody = AlarmMessageRenderer.render(AlarmType.CURATION_UPDATED.getBody(), payload.toValues());

            Alarm alarm = alarmRepository.save(Alarm.create(AlarmType.CURATION_UPDATED, item.curationId(), renderedBody));
            userAlarmRepository.save(UserAlarm.create(userRepository.getReferenceById(item.userId()), alarm));

            targets.add(new FcmBulkTarget(
                    item.userId(),
                    FcmSendRequestDTO.withValues(
                            AlarmType.CURATION_UPDATED, item.curationId(), alarm.getId(), payload.toValues())));
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new CurationAlarmBulkEvent(targets));
            }
        });
    }
}
