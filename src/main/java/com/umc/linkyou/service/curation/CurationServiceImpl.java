package com.umc.linkyou.service.curation;

import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.service.common.KeywordNameResolver;
import com.umc.linkyou.service.curation.ment.CurationMentMaterializer;
import com.umc.linkyou.service.curation.utils.ThumbnailUrlProvider;
import com.umc.linkyou.service.curation.linku.external.ExternalRecommendMaterializer;
import com.umc.linkyou.service.curation.linku.internal.InternalRecommendMaterializer;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
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
import org.springframework.data.domain.PageRequest;

import java.time.YearMonth;
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
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final KeywordNameResolver keywordNameResolver;

    // 모든 유저 호출한 시점의 이전 달에 해당하는 큐레이션 생성
    @Override
    @Transactional
    public void generateMonthlyCurationForAllUsers() {
        // 이전 달 설정
        String month = YearMonth.now().minusMonths(1).toString();

        // 모든 유저에 대해 생성
        List<Users> users = userRepository.findAll();
        for (Users user : users) {
            doGenerateCuration(user, month);
        }
    }

    // 단일 유저의 특정 월 큐레이션 생성
    @Override
    @Transactional
    public void generateCurationForUser(Long userId, String month) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._USER_NOT_FOUND));
        doGenerateCuration(user, month);
    }

    // 큐레이션 생성
    private void doGenerateCuration(Users user, String month) {
        // 같은 유저, 같은 달 큐레이션이 이미 있으면 중복 생성을 막음
        if (curationRepository.existsByUserAndMonth(user, month)) {
            log.info("[Curation] skip userId={} month={} (already exists)", user.getId(), month);
            return;
        }

        // 큐레이션 레코드 저장
        Curation curation = Curation.builder()
                .user(user)
                .month(month)
                .build();
        curationRepository.save(curation);

        // 멘트, 내/외부 링크 추천 생성
        Long cid = curation.getCurationId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                curationMentMaterializer.generateAndStoreMentAsync(cid);
                internalRecommendMaterializer.generateAndStoreInternalAsync(cid);
                externalRecommendMaterializer.generateAndStoreExternalAsync(cid);
            }
        });
    }

    // 유저의 최근 큐레이션 정보를 가져옴
    @Override
    @Transactional(readOnly = true)
    public Optional<CurationLatestResponse> getLatestCuration(Long userId) {
        return curationRepository.findTopByUser_IdOrderByMonthDesc(userId)
                .map(curation -> CurationLatestResponse.builder()
                        .curationId(curation.getCurationId())
                        .month(curation.getMonth())
                        .thumbnailUrl(thumbnailUrlProvider.getUrlForMonth("curation", curation.getMonth()))
                        .build());
    }

    // 유저의 큐레이션을 detail 정보를 가져옴
    @Override
    @Transactional(readOnly = true)
    public CurationDetailResponse getCurationDetail(Long userId, Long curationId) {
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new GeneralException(ErrorStatus._CURATION_NOT_FOUND));

        if (!curation.getUser().getId().equals(userId)) {
            throw new GeneralException(ErrorStatus._CURATION_FORBIDDEN);
        }

        String baseMonth = curation.getMonth();

        // 상위 태그 3개 조회
        List<String> tagNames = keywordMonthlyCountRepository
                .findTopByUserIdAndBaseMonth(userId, baseMonth, PageRequest.of(0, 3))
                .stream()
                .map(kmc -> keywordNameResolver.resolve(kmc.getType(), kmc.getRefId()))
                .toList();

        return CurationDetailResponse.builder()
                .curationId(curation.getCurationId())
                .month(curation.getMonth())
                .topTags(tagNames)
                .headerMent(curation.getHeaderMent())
                .footerMent(curation.getFooterMent())
                .mentReady(curation.getHeaderMent() != null && curation.getFooterMent() != null)
                .build();
    }

    // 연도별 12개 큐레이션 히스토리 (없는 달은 빈 상태)
    @Override
    @Transactional(readOnly = true)
    public List<CurationListResponse> getMyCurationList(Long userId, int year) {
        String yearPrefix = year + "-";

        Map<String, Curation> existing = curationRepository
                .findAllByUser_IdAndMonthStartingWith(userId, yearPrefix)
                .stream()
                .collect(Collectors.toMap(Curation::getMonth, Function.identity()));

        List<CurationListResponse> result = new ArrayList<>(12);
        for (int m = 1; m <= 12; m++) {
            String month = String.format("%d-%02d", year, m);
            Curation c = existing.get(month);
            if (c != null) {
                result.add(CurationListResponse.builder()
                        .curationId(c.getCurationId())
                        .month(month)
                        .thumbnailUrl(thumbnailUrlProvider.getUrlForMonth("curation", month))
                        .build());
            } else {
                result.add(CurationListResponse.builder()
                        .month(month)
                        .build());
            }
        }
        return result;
    }

    // 월별 섹션 정보 조회 (제목, 설명, 대표 이미지)
    @Override
    @Transactional(readOnly = true)
    public List<CurationSectionResponse> getSectionInfo(String month) {
        return curationSectionInfoRepository
                .findAllByMonthOrderBySectionNumberAsc(month)
                .stream()
                .map(s -> CurationSectionResponse.builder()
                        .section(s.getSectionNumber())
                        .title(s.getTitle())
                        .description(s.getDescription())
                        .imageUrl(s.getImageUrl())
                        .build())
                .toList();
    }
}
