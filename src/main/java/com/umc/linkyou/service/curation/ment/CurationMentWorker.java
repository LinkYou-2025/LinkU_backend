package com.umc.linkyou.service.curation.ment;

import com.umc.linkyou.apiPayload.code.status.curation.CurationErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.CurationMent;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.domain.log.KeywordMonthlyCount;
import com.umc.linkyou.infra.ai.AiMentService;
import com.umc.linkyou.infra.ai.dto.MentResultDTO;
import com.umc.linkyou.repository.curationRepository.CurationMentRepository;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.service.common.EmotionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurationMentWorker {

    private final CurationRepository curationRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final EmotionMapper emotionMapper;
    private final AiMentService aiMentService;
    private final CurationMentRepository curationMentRepository;

    @Transactional
    public void generateMent(Long curationId) {
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new GeneralException(CurationErrorStatus._CURATION_NOT_FOUND));

        Users user = curation.getUser();
        Long userId = user.getId();
        String nickname = user.getNickName();
        // curation.baseMonth는 이슈 월(N). 데이터 집계 기간은 N-1월이다.
        String dataBaseMonth = YearMonth.parse(curation.getBaseMonth()).minusMonths(1).toString();

        // 상위 1개 감정 조회
        Optional<KeywordMonthlyCount> topEmotionCount = keywordMonthlyCountRepository
                .findTopByUserIdAndBaseMonthAndType(userId, dataBaseMonth, KeywordType.EMOTION, PageRequest.of(0, 1))
                .stream().findFirst();

        String emotionName;
        Long emotionId;
        // 월에 1번만 기록된 감정은 평온으로 대체
        if (topEmotionCount.isEmpty() || topEmotionCount.get().getCount() < 2) {
            emotionName = "평온";
            emotionId = emotionMapper.getEmotionId("평온");
        } else {
            emotionId = topEmotionCount.get().getRefId();
            emotionName = emotionMapper.getEmotionName(emotionId);
        }

        String header = null;
        String footer = null;

        try {
            MentResultDTO result = aiMentService.generateMent(emotionName);
            header = result.header();
            footer = result.footer();
        } catch (Exception e) {
            log.warn("[AI 멘트 생성 실패] curationId={}, cause={}", curationId, e.getMessage());
        }

        // AI 생성 실패하면 DB에서 가져다가 멘트 저장
        if (header == null || header.isBlank() || footer == null || footer.isBlank()) {
            List<CurationMent> mentList = curationMentRepository.findAllByEmotionId(emotionId);
            if (!mentList.isEmpty()) {
                CurationMent fallback = mentList.get(ThreadLocalRandom.current().nextInt(mentList.size()));
                header = fallback.getHeaderText();
                footer = fallback.getFooterText();
            }
        }

        // 폴백까지 실패하면 mentReady=false
        if (header != null && footer != null) {
            curation.updateMent(
                    header.replace("(닉네임)", nickname),
                    footer.replace("(닉네임)", nickname)
            );
        }
    }
}