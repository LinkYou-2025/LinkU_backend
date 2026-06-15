package com.umc.linkyou.batch.curation;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.curation.CurationBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * 큐레이션 생성 프로세서
 * - 각 사용자에 대해 큐레이션 생성 여부를 판단
 * - 대상이 아니라면 null 반환하여 해당 사용자는 skip 처리
 * - 대상이라면 월/썸네일이 포함된 배치 아이템을 writer로 전달
 */
@Component
@RequiredArgsConstructor
public class CurationItemProcessor implements ItemProcessor<Users, MonthlyCurationBatchItem> {

    private final CurationBatchService curationBatchService;

    @Override
    public MonthlyCurationBatchItem process(Users user) {
        return curationBatchService.toBatchItem(user).orElse(null);
    }
}
