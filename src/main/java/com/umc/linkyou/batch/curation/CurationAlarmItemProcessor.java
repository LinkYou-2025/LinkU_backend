package com.umc.linkyou.batch.curation;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class CurationAlarmItemProcessor implements ItemProcessor<CurationAlarmCandidate, CurationAlarmBatchItem> {

    @Override
    public CurationAlarmBatchItem process(CurationAlarmCandidate candidate) {
        return new CurationAlarmBatchItem(candidate.userId(), candidate.curationId(), candidate.nickname());
    }
}
