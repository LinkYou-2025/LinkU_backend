package com.umc.linkyou.service.curation;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.CurationMent;
import com.umc.linkyou.domain.log.CurationTopLog;
import com.umc.linkyou.repository.CurationMentRepository;
import com.umc.linkyou.service.curation.gpt.GptService;
import com.umc.linkyou.service.curation.utils.ThumbnailUrlProvider;
import com.umc.linkyou.service.curation.linku.ExternalRecommendMaterializer;
import com.umc.linkyou.web.dto.curation.CreateCurationRequest;
import com.umc.linkyou.repository.LogRepository.CurationTopLogRepository;
import com.umc.linkyou.repository.CurationRepository;
import com.umc.linkyou.repository.userRepository.UserRepository;
import com.umc.linkyou.web.dto.curation.CurationDetailResponse;
import com.umc.linkyou.web.dto.curation.CurationLatestResponse;
import com.umc.linkyou.web.dto.curation.GptMentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class CurationServiceImpl implements CurationService {

    private final UserRepository userRepository;
    private final CurationRepository curationRepository;
    private final CurationTopLogService curationTopLogService;
    private final ThumbnailUrlProvider thumbnailUrlProvider;
    private final ExternalRecommendMaterializer externalRecommendMaterializer;

    /**
     * 유저의 큐레이션을 생성하고, 감정/상황 로그 기반 top3 태그를 계산해 저장한다.
     */
    @Override
    @Transactional
    public Curation createCuration(Long userId, CreateCurationRequest request) {
        // 1. 사용자 조회
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1-2. 썸네일 URL 생성
        String thumbnailUrl = thumbnailUrlProvider.getUrlForMonth("curation", request.getMonth());

        // 2. 큐레이션 객체 생성
        Curation curation = Curation.builder()
                .user(user)
                .month(request.getMonth())
                .thumbnailUrl(thumbnailUrl)
                .build();

        // 3. 저장
        curationRepository.save(curation);

        // 4. 상위 3개 로그 저장
        curationTopLogService.calculateAndSaveTopLogs(userId, curation);

        return curation;
    }
    /**
     * 유저의 큐레이션을 자동생성
     */
    @Override
    @Transactional
    public void generateMonthlyCurationForAllUsers() {
        YearMonth prevMonth = YearMonth.now().minusMonths(1);
        String month = prevMonth.toString();
        String thumbnailUrl = thumbnailUrlProvider.getUrlForMonth("curation", month);

        List<Users> users = userRepository.findAll();
        for (Users user : users) {
            if (curationRepository.existsByUserAndMonth(user, month)) continue;

            Curation curation = Curation.builder()
                    .user(user)
                    .month(month)
                    .thumbnailUrl(thumbnailUrl)
                    .build();

            curationRepository.save(curation);
            curationTopLogService.calculateAndSaveTopLogs(user.getId(), curation);

            // ✅ 커밋 이후에 외부추천 비동기 실행 (레이스 방지)
            Long cid = curation.getCurationId();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    externalRecommendMaterializer.generateAndStoreExternalAsync(cid);
                }
            });
        }
    }

    /**
     * 기존 운영 코드에는 손대지 않고, 개발용으로만 2025-02 ~ 2025-07 생성
     * - idempotent: existsByUserAndMonth 로 중복 방지
     * - materializeExternal=false 권장(외부 호출 부담 줄이기)
     */
    @Override
    @Transactional
    public void seedFebToJul2025(boolean materializeExternal) {
        List<Users> users = userRepository.findAll();

        YearMonth cursor = YearMonth.of(2025, 2);
        YearMonth end = YearMonth.of(2025, 7);

        while (!cursor.isAfter(end)) {
            final String monthStr = cursor.toString(); // "YYYY-MM"
            final String thumbnailUrl = thumbnailUrlProvider.getUrlForMonth("curation", monthStr);

            for (Users user : users) {
                if (curationRepository.existsByUserAndMonth(user, monthStr)) continue;

                Curation curation = Curation.builder()
                        .user(user)
                        .month(monthStr)
                        .thumbnailUrl(thumbnailUrl)
                        .build();

                curationRepository.save(curation);

                // Top 로그 계산/저장 (현행 규칙 동일)
                curationTopLogService.calculateAndSaveTopLogs(user.getId(), curation);

                if (materializeExternal) {
                    // ✅ 커밋 이후로 지연 실행
                    Long cid = curation.getCurationId();
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override public void afterCommit() {
                            externalRecommendMaterializer.generateAndStoreExternalAsync(cid);
                        }
                    });
                }
            }

            cursor = cursor.plusMonths(1);
        }
    }

    private final CurationTopLogRepository curationTopLogRepository;
    private final CurationMentRepository curationMentRepository;
    private final GptService gptService;


    /**
     * 유저의 큐레이션을 detail 정보를 가져옴
     */
    @Override
    @Transactional(readOnly = true)
    public CurationDetailResponse getCurationDetail(Long curationId) {
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("큐레이션 없음"));

        // 0. 사용자 닉네임 가져오기
        String nickname = curation.getUser().getNickName(); // 연관관계 매핑 필요

        // 1. 상위 태그 3개 조회
        List<CurationTopLog> topLogs = curationTopLogRepository.findTop3ByCurationId(curationId);
        List<String> tagNames = topLogs.stream()
                .map(CurationTopLog::getTagName)
                .toList();

        // 2. 감정 기반 로그 중 count 가장 높은 것 추출 (top3에 없어도 상관없게)
        CurationTopLog topEmotionLog = curationTopLogRepository.findTopEmotionLogByCurationId(curationId);

        String emotionName;
        Long emotionId = null;

        if (topEmotionLog == null || topEmotionLog.getCount() < 2) {
            emotionName = "평온";
        } else {
            emotionName = topEmotionLog.getTagName();
            emotionId = topEmotionLog.getRefId(); // fallback 용
        }

        String header = null;
        String footer = null;

        // GPT 기반 멘트 요청
        GptMentResponse gptResponse = gptService.generateMent(emotionName);

        if (gptResponse != null) {
            header = gptResponse.getHeader();
            footer = gptResponse.getFooter();
        }

        // ❗ 실패 시 DB fallback (원래 멘트 추천로직)
        if (header == null || footer == null) {
            List<CurationMent> mentList = curationMentRepository.findAllByEmotion_EmotionId(emotionId);
            if (mentList.isEmpty()) throw new IllegalArgumentException("멘트 없음");
            CurationMent fallback = mentList.get(new Random().nextInt(mentList.size()));

            header = fallback.getHeaderText();
            footer = fallback.getFooterText();
        }

        // 4. (닉네임) 치환
        header = header.replace("(닉네임)", nickname);
        footer = footer.replace("(닉네임)", nickname);

        // 5. 응답 반환
        return CurationDetailResponse.builder()
                .curationId(curation.getCurationId())
                .month(curation.getMonth())
                .topTags(tagNames)
                .headerMent(header)
                .footerMent(footer)
                .build();
    }


    /**
     * 유저의 최근 큐레이션 정보를 가져옴
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<CurationLatestResponse> getLatestCuration(Long userId) {
        return curationRepository.findTopByUser_IdOrderByCreatedAtDesc(userId)
                .map(curation -> new CurationLatestResponse(
                        curation.getCurationId(),
                        curation.getMonth(),         // YearMonth → "2025-07"
                        curation.getThumbnailUrl()
                ));
    }

}
