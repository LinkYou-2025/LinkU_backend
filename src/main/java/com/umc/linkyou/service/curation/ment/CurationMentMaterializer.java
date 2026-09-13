package com.umc.linkyou.service.curation.ment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

@Component
@RequiredArgsConstructor
@Slf4j
public class CurationMentMaterializer {

    private final CurationMentWorker worker;
    private final Semaphore mentLimiter;

    @Async("mentTaskExecutor")
    public void generateMentAsync(Long curationId) {
        boolean acquired = false;
        try {
            mentLimiter.acquire();
            acquired = true;
            worker.generateMent(curationId);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("멘트 생성 인터럽트 curationId={}", curationId);
        } catch (Exception e) {
            log.error("멘트 생성 실패 curationId={}", curationId, e);
        } finally {
            if (acquired) mentLimiter.release();
        }
    }
}