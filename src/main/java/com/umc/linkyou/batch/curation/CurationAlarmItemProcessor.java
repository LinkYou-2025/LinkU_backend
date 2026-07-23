package com.umc.linkyou.batch.curation;

import com.umc.linkyou.domain.enums.AlarmType;
import com.umc.linkyou.repository.AlarmSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * 큐레이션 알림 발송 프로세서
 * - 알림 설정이 꺼져 있는 유저는 null을 반환해 skip 처리
 */
@Component
@RequiredArgsConstructor
public class CurationAlarmItemProcessor implements ItemProcessor<CurationAlarmCandidate, CurationAlarmBatchItem> {

    private final AlarmSettingRepository alarmSettingRepository;

    @Override
    public CurationAlarmBatchItem process(CurationAlarmCandidate candidate) {
        boolean enabled = alarmSettingRepository.findByUserId(candidate.userId())
                .map(setting -> setting.isEnabled(AlarmType.CURATION_UPDATED.getSettingType()))
                .orElse(false);

        if (!enabled) {
            return null;
        }

        return new CurationAlarmBatchItem(candidate.userId(), candidate.curationId(), candidate.nickname());
    }
}
