package com.umc.linkyou.service.domain;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.enums.DomainSource;
import com.umc.linkyou.repository.classification.domainRepository.DomainRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DomainFaviconWorker {

    private static final int RETRY_AFTER_DAYS = 7;

    private final DomainRepository domainRepository;
    private final DomainFaviconService domainFaviconService;

    // 실패한 도메인은 RETRY_AFTER_DAYS가 지난 뒤 다시 시도한다.
    @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Seoul")
    public void refreshMissingAutoDomainFavicons() {
        List<Long> domainIds = domainRepository.findAutoDomainIdsMissingFavicon();
        domainIds.forEach(this::processOneSafely);
        log.info("[파비콘] AUTO 도메인 {}건 수집 완료", domainIds.size());
    }

    private void processOneSafely(Long domainId) {
        try {
            processOne(domainId);
        } catch (Exception e) {
            log.warn("[파비콘] domainId={} 처리 실패", domainId, e);
        }
    }

    private void processOne(Long domainId) {
        Domain domain = domainRepository.findById(domainId).orElse(null);
        // 조회와 처리 사이에 관리자가 바꿨을 수 있어 다시 확인한다.
        if (domain == null
                || domain.getSource() != DomainSource.AUTO
                || domain.getImageUrl() != null) {
            return;
        }

        domainFaviconService
                .collect(domain)
                .ifPresentOrElse(
                        domain::updateFavicon,
                        () ->
                                domain.markFaviconFailed(
                                        LocalDateTime.now().plusDays(RETRY_AFTER_DAYS)));
        domainRepository.save(domain);
    }
}
