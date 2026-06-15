package com.umc.linkyou.service.curation.listener;

import com.umc.linkyou.service.curation.event.MonthlyCurationCreatedEvent;
import com.umc.linkyou.service.curation.linku.ExternalRecommendMaterializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 월간 큐레이션 생성 이후 외부 추천 후처리를 담당하는 리스너다.
 *
 * <p>트랜잭션 커밋 이후에만 비동기로 실행되어, 저장이 롤백된 큐레이션에 대해
 * 잘못된 외부 추천 생성이 일어나지 않도록 보호한다.
 */
@Component
@RequiredArgsConstructor
public class MonthlyCurationCreatedEventListener {

    private final ExternalRecommendMaterializer externalRecommendMaterializer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(MonthlyCurationCreatedEvent event) {
        externalRecommendMaterializer.generateAndStoreExternalAsync(event.curationId());
    }
}
