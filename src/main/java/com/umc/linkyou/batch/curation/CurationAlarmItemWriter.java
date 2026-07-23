package com.umc.linkyou.batch.curation;

import com.umc.linkyou.service.curation.CurationAlarmBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * 큐레이션 알림 발송 writer
 *
 * <p>청크(최대 100건) 단위로 Alarm/UserAlarm 저장과 FCM 일괄 발송을 배치 전용 서비스에 위임
 */
@Component
@RequiredArgsConstructor
public class CurationAlarmItemWriter implements ItemWriter<CurationAlarmBatchItem> {

    private final CurationAlarmBatchService curationAlarmBatchService;

    @Override
    public void write(Chunk<? extends CurationAlarmBatchItem> chunk) {
        curationAlarmBatchService.sendCurationAlarms(chunk.getItems());
    }
}
