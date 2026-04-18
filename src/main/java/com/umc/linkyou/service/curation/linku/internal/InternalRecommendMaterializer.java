package com.umc.linkyou.service.curation.linku.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalRecommendMaterializer {

    private final InternalRecommendWorker worker;
    private final Semaphore internalRecoLimiter;

    @Async("defaultTaskExecutor")
    public void generateAndStoreInternalAsync(Long curationId) {
        boolean acquired = false;
        try {
            internalRecoLimiter.acquire();
            acquired = true;
            worker.generateAndStoreInternal(curationId);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("internal recommend interrupted for curationId={}", curationId);
        } catch (Exception e) {
            log.error("internal recommend failed for curationId={}", curationId, e);
        } finally {
            if (acquired) internalRecoLimiter.release();
        }
    }
}
