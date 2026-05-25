package com.umc.linkyou.service.curation;

import com.umc.linkyou.repository.userRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.util.List;

// TODO: 지원ver 배치로 변경 필요
@Slf4j
@Component
@RequiredArgsConstructor
public class CurationBatchScheduler {

    private final CurationService curationService;
    private final UserRepository userRepository;

    private static final int BATCH_SIZE = 30;

    @Scheduled(cron = "0 5 0 1 * *", zone = "Asia/Seoul")
    public void runMonthlyCurationBatch() {
        String month = YearMonth.now().minusMonths(1).toString();
        log.info("[CurationBatch] 월간 큐레이션 자동 생성 시작: month={}", month);

        int total = 0, success = 0, skipped = 0, failed = 0;
        int page = 0;

        while (true) {
            List<Long> userIds = userRepository.findAllIds(PageRequest.of(page++, BATCH_SIZE));
            if (userIds.isEmpty()) break;
            total += userIds.size();

            for (Long userId : userIds) {
                try {
                    boolean created = curationService.generateCurationForUser(userId, month);
                    if (created) success++;
                    else skipped++;
                } catch (Exception e) {
                    log.warn("[CurationBatch] userId={} 처리 실패: {}", userId, e.getMessage());
                    failed++;
                }
            }
        }

        log.info("[CurationBatch] 완료: 총{}명 / 성공{}명 / 스킵{}명 / 실패{}명",
                total, success, skipped, failed);
    }
}
