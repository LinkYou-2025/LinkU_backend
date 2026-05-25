package com.umc.linkyou.service.curation.recommend.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalRecommendMaterializer {

    private final ExternalRecommendWorker worker;
    private final Semaphore externalRecoLimiter;

    @Async("defaultTaskExecutor")
    public void generateExternalAsync(Long curationId) {
        boolean acquired = false;
        try {
            externalRecoLimiter.acquire();
            acquired = true;
            worker.generateExternal(curationId);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("외부 추천 인터럽트 curationId={}", curationId);
        } catch (Exception e) {
            log.error("외부 추천 실패 curationId={}", curationId, e);
        } finally {
            if (acquired) {
                externalRecoLimiter.release();
            }
        }
    }
}
