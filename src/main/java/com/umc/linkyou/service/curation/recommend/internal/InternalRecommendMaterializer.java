package com.umc.linkyou.service.curation.recommend.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalRecommendMaterializer {

    private final InternalRecommendWorker worker;
    private final Semaphore internalRecoLimiter;

    @Async("internalRecoTaskExecutor")
    public void generateInternalAsync(Long curationId) {
        boolean acquired = false;
        try {
            internalRecoLimiter.acquire();
            acquired = true;
            worker.generateInternal(curationId);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("내부 추천 인터럽트 curationId={}", curationId);
        } catch (Exception e) {
            log.error("내부 추천 실패 curationId={}", curationId, e);
        } finally {
            if (acquired) internalRecoLimiter.release();
        }
    }
}
