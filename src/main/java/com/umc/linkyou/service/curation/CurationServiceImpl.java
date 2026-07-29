package com.umc.linkyou.service.curation;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.service.curation.ment.CurationMentMaterializer;
import com.umc.linkyou.service.curation.utils.ThumbnailUrlProvider;
import com.umc.linkyou.service.curation.recommend.external.ExternalRecommendMaterializer;
import com.umc.linkyou.service.curation.recommend.internal.InternalRecommendMaterializer;
import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.curationRepository.CurationSectionInfoRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import com.umc.linkyou.web.dto.curation.CurationLatestResponse;
import com.umc.linkyou.web.dto.curation.CurationSectionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.umc.linkyou.web.dto.curation.CurationListResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurationServiceImpl implements CurationService {

    private final UserRepository userRepository;
    private final CurationRepository curationRepository;
    private final CurationSectionInfoRepository curationSectionInfoRepository;
    private final ThumbnailUrlProvider thumbnailUrlProvider;
    private final ExternalRecommendMaterializer externalRecommendMaterializer;
    private final InternalRecommendMaterializer internalRecommendMaterializer;
    private final CurationMentMaterializer curationMentMaterializer;

    // 단일 유저의 특정 월 큐레이션 생성
    @Override
    @Transactional
    public boolean generateCurationForUser(Long userId, String month) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));
        return doGenerateCuration(user, month);
    }

    private boolean doGenerateCuration(Users user, String month) {
        if (curationRepository.existsByUserAndBaseMonth(user, month)) {
            return false;
        }

        Curation curation = Curation.builder()
                .user(user)
                .baseMonth(month)
                .build();
        curationRepository.save(curation);

        Long cid = curation.getCurationId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                curationMentMaterializer.generateMentAsync(cid);
                internalRecommendMaterializer.generateInternalAsync(cid);
                externalRecommendMaterializer.generateExternalAsync(cid);
            }
        });
        return true;
    }

    // 연도별 12개 큐레이션 히스토리 (없는 달은 빈 상태)
    @Override
    @Transactional(readOnly = true)
    public List<CurationListResponse> getCurationList(Long userId, int year) {
        if (year < 2025) {
            throw new GeneralException(CurationErrorStatus._CURATION_INVALID_YEAR);
        }
        Map<String, Curation> existing = curationRepository
                .findAllByUserIdAndYear(userId, String.valueOf(year))
                .stream()
                .collect(Collectors.toMap(Curation::getBaseMonth, Function.identity()));

        List<CurationListResponse> result = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            String month = String.format("%d-%02d", year, m);
            Curation c = existing.get(month);
            String thumbnailUrl = c != null ? getSection1ImageUrl(month) : null;
            result.add(CurationConverter.toCurationListResponse(c, month, thumbnailUrl));
        }
        return result;
    }

    // 유저의 최근 큐레이션 정보를 가져옴
    @Override
    @Transactional(readOnly = true)
    public Optional<CurationLatestResponse> getLatestCuration(Long userId) {
        return curationRepository.findLatestByUserId(userId)
                .map(curation -> CurationConverter.toCurationLatestResponse(
                        curation, getSection1ImageUrl(curation.getBaseMonth())));
    }

    private String getSection1ImageUrl(String month) {
        return curationSectionInfoRepository.findByMonthAndSectionNumber(month, 1)
                .map(CurationSectionInfo::getImageUrl)
                .orElse(null);
    }

    // 월별 섹션 정보 조회 (제목, 설명, 대표 이미지)
    @Override
    @Transactional(readOnly = true)
    public List<CurationSectionResponse> getCurationSections(String month) {
        return curationSectionInfoRepository
                .findAllByMonth(month)
                .stream()
                .map(CurationConverter::toCurationSectionResponse)
                .toList();
    }

    // 유저의 큐레이션 detail 정보를 가져옴
    @Override
    @Transactional(readOnly = true)
    public CurationDetailResponse getCurationDetail(Long userId, Long curationId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(UserErrorStatus._USER_NOT_FOUND));

        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new GeneralException(CurationErrorStatus._CURATION_NOT_FOUND));

        if (!curation.getUser().getId().equals(user.getId())) {
            throw new GeneralException(CurationErrorStatus._CURATION_FORBIDDEN);
        }

        return CurationDetailResponse.builder()
                .curationId(curation.getCurationId())
                .month(curation.getBaseMonth())
                .headerMent(curation.getHeaderMent())
                .footerMent(curation.getFooterMent())
                .mentReady(curation.getHeaderMent() != null && !curation.getHeaderMent().isBlank()
                        && curation.getFooterMent() != null && !curation.getFooterMent().isBlank())
                .build();
    }
}
