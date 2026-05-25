package com.umc.linkyou.service.curation.recommend.internal;

import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.CurationLinkuType;
import com.umc.linkyou.domain.mapping.CurationLinku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.curationRepository.CurationLinkuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InternalRecommendWorker {

    private final CurationRepository curationRepository;
    private final CurationLinkuRepository curationLinkuRepository;
    private final InternalLinkCandidateService internalLinkCandidateService;

    @Transactional
    public void generateInternal(Long curationId) {
        log.info("[INT] 내부 추천 시작 curationId={}", curationId);

        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new GeneralException(CurationErrorStatus._CURATION_NOT_FOUND));
        Users user = curation.getUser();
        Long userId = user.getId();

        List<UsersLinku> candidates = internalLinkCandidateService.getInternalCandidates(userId, curationId, 4);

        curationLinkuRepository.deleteAllByCurationIdAndType(curationId, CurationLinkuType.RECOMMENDED);

        List<CurationLinku> toSave = candidates.stream()
                .map(item -> CurationLinku.ofInternal(curation, item, item.getImageUrl()))
                .toList();
        curationLinkuRepository.saveAll(toSave);

        log.info("[INT] 내부 추천 저장 완료 curationId={}, rows={}", curationId, toSave.size());
    }
}
