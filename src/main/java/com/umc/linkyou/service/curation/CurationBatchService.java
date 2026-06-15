package com.umc.linkyou.service.curation;

import com.umc.linkyou.batch.curation.MonthlyCurationBatchItem;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.CurationRepository;
import com.umc.linkyou.service.curation.event.MonthlyCurationCreatedEvent;
import com.umc.linkyou.service.curation.utils.ThumbnailUrlProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 월간 큐레이션 배치 전용 서비스
 *
 * <p>reader가 읽은 사용자를 배치 생성 대상으로 선별하고, writer가 넘긴 아이템 목록을
 * 실제 큐레이션 저장과 후처리 이벤트 발행으로 연결한다.
 */
@Service
@RequiredArgsConstructor
public class CurationBatchService {

    private final CurationRepository curationRepository;
    private final CurationTopLogService curationTopLogService;
    private final ThumbnailUrlProvider thumbnailUrlProvider;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 사용자가 이번 월간 큐레이션 생성 대상인지 판단하고,
     * 대상이면 writer로 넘길 배치 아이템을 생성
     */
    @Transactional(readOnly = true)
    public Optional<MonthlyCurationBatchItem> toBatchItem(Users user) {
        YearMonth previousMonth = YearMonth.now().minusMonths(1);
        String month = previousMonth.toString();

        if (curationRepository.existsByUserAndMonth(user, month)) {
            return Optional.empty();
        }

        String thumbnailUrl = thumbnailUrlProvider.getUrlForMonth("curation", previousMonth);
        return Optional.of(new MonthlyCurationBatchItem(user, month, thumbnailUrl));
    }

    /**
     * processor가 선별한 배치 아이템 목록으로 큐레이션을 생성
     */
    @Transactional
    public void createMonthlyCurations(List<? extends MonthlyCurationBatchItem> items) {
        for (MonthlyCurationBatchItem item : items) {
            Curation curation = Curation.builder()
                    .user(item.user())
                    .month(item.month())
                    .thumbnailUrl(item.thumbnailUrl())
                    .build();

            curationRepository.save(curation);
            curationTopLogService.calculateAndSaveTopLogs(item.user().getId(), curation);
            eventPublisher.publishEvent(new MonthlyCurationCreatedEvent(curation.getCurationId()));
        }
    }
}
