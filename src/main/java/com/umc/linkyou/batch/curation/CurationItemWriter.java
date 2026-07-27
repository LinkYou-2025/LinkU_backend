package com.umc.linkyou.batch.curation;

import com.umc.linkyou.service.curation.CurationBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * 월간 큐레이션 생성 writer
 *
 * 실제 로직은 배치 전용 서비스로 위임함
 */
@Component
@RequiredArgsConstructor
public class CurationItemWriter implements ItemWriter<MonthlyCurationBatchItem> {

    private final CurationBatchService curationBatchService;

    @Override
    public void write(Chunk<? extends MonthlyCurationBatchItem> chunk) {
        curationBatchService.createMonthlyCurations(chunk.getItems());
    }
}
