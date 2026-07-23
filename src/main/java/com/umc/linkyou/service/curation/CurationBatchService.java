package com.umc.linkyou.service.curation;

import com.umc.linkyou.batch.curation.MonthlyCurationBatchItem;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.service.curation.ment.CurationMentMaterializer;
import com.umc.linkyou.service.curation.recommend.external.ExternalRecommendMaterializer;
import com.umc.linkyou.service.curation.recommend.internal.InternalRecommendMaterializer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 월간 큐레이션 배치 전용 서비스
 *
 * <p>reader가 읽은 사용자를 배치 생성 대상으로 선별하고, writer가 넘긴 아이템 목록을
 * 실제 큐레이션 저장과 후처리(멘트/추천 생성)로 연결한다.
 * {@link CurationServiceImpl#generateCurationForUser}와 동일한 생성 절차를 따른다.
 */
@Service
@RequiredArgsConstructor
public class CurationBatchService {

    private final CurationRepository curationRepository;
    private final CurationMentMaterializer curationMentMaterializer;
    private final InternalRecommendMaterializer internalRecommendMaterializer;
    private final ExternalRecommendMaterializer externalRecommendMaterializer;

    /**
     * 사용자가 이번 월간 큐레이션 생성 대상인지 판단하고,
     * 대상이면 writer로 넘길 배치 아이템을 생성
     */
    @Transactional(readOnly = true)
    public Optional<MonthlyCurationBatchItem> toBatchItem(Users user) {
        String baseMonth = YearMonth.now().minusMonths(1).toString();

        if (curationRepository.existsByUserAndBaseMonth(user, baseMonth)) {
            return Optional.empty();
        }

        return Optional.of(new MonthlyCurationBatchItem(user, baseMonth));
    }

    /**
     * processor가 선별한 배치 아이템 목록으로 큐레이션을 생성하고,
     * 커밋 이후 멘트/내부 추천/외부 추천 생성을 비동기로 트리거함
     */
    @Transactional
    public void createMonthlyCurations(List<? extends MonthlyCurationBatchItem> items) {
        for (MonthlyCurationBatchItem item : items) {
            Curation curation = Curation.builder()
                    .user(item.user())
                    .baseMonth(item.baseMonth())
                    .build();

            curationRepository.save(curation);

            Long curationId = curation.getCurationId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    curationMentMaterializer.generateMentAsync(curationId);
                    internalRecommendMaterializer.generateInternalAsync(curationId);
                    externalRecommendMaterializer.generateExternalAsync(curationId);
                }
            });
        }
    }
}
