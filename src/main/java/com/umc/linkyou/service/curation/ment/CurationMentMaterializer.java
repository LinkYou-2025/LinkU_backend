package com.umc.linkyou.service.curation.ment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.Semaphore;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurationMentMaterializer {

    private final CurationMentWorker worker;
    private final Semaphore mentLimiter;

    @Async("defaultTaskExecutor")
    public void generateAndStoreMentAsync(Long curationId) {
        boolean acquired = false;
        try {
            mentLimiter.acquire();
            acquired = true;
            worker.generateAndStoreMent(curationId);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("ment generation interrupted for curationId={}", curationId);
        } catch (Exception e) {
            log.error("ment generation failed for curationId={}", curationId, e);
        } finally {
            if (acquired) mentLimiter.release();
        }
    }
}
