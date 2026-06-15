package com.umc.linkyou.batch.curation;

import com.umc.linkyou.service.curation.CurationBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * 월간 큐레이션 생성 writer
 *
 * <p>실제 큐레이션 저장, top log 계산, 커밋 이후 외부 추천 실행은
 * 배치 전용 서비스로 위임한다.
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
