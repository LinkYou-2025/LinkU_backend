package com.umc.linkyou.service.curation.linku;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalRecommendMaterializer {

    private final ExternalRecommendWorker worker;     // ✅ 분리된 트랜잭션 빈 주입
    private final Semaphore externalRecoLimiter;      // 동시 실행 제한

    /** 비동기 래퍼: 동시성 제한 + 트랜잭션은 Worker에서 처리 */
    @Async("defaultTaskExecutor")
    public void generateAndStoreExternalAsync(Long curationId) {
        log.info("[EXT] async trigger curationId={}", curationId);
        boolean acquired = false;
        try {
            externalRecoLimiter.acquire();
            acquired = true;
            worker.generateAndStoreExternal(curationId);  // ✅ 다른 빈 호출 → @Transactional 적용됨
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("external recommend interrupted for curationId={}", curationId);
            return;
        } catch (Exception e) {
            log.error("external recommend failed for curationId={}", curationId, e);
        } finally {
            if (acquired) {
                externalRecoLimiter.release();
            }
        }
    }
}
