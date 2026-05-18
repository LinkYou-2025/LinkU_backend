package com.umc.linkyou.service.curation.ment;

import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.classification.CurationMent;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.infra.ai.AiMentService;
import com.umc.linkyou.repository.curationRepository.CurationMentRepository;
import com.umc.linkyou.repository.curationRepository.CurationRepository;
import com.umc.linkyou.repository.keywordRepository.KeywordMonthlyCountRepository;
import com.umc.linkyou.service.common.EmotionTagMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurationMentWorker {

    private final CurationRepository curationRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final EmotionTagMapper emotionTagMapper;
    private final AiMentService aiMentService;
    private final CurationMentRepository curationMentRepository;

    @Transactional
    public void generateAndStoreMent(Long curationId) {
        Curation curation = curationRepository.findById(curationId)
                .orElseThrow(() -> new IllegalArgumentException("curation not found"));

        Long userId = curation.getUser().getId();
        String nickname = curation.getUser().getNickName();
        String baseMonth = curation.getMonth();

        var topEmotionCount = keywordMonthlyCountRepository
                .findTopByUserIdAndBaseMonthAndType(userId, baseMonth, KeywordType.EMOTION, PageRequest.of(0, 1))
                .stream().findFirst().orElse(null);

        String emotionName;
        Long emotionId = null;
        if (topEmotionCount == null || topEmotionCount.getCount() < 2) {
            emotionName = "평온";
        } else {
            emotionName = emotionTagMapper.getEmotionName(topEmotionCount.getRefId());
            emotionId = topEmotionCount.getRefId();
        }

        String header = null;
        String footer = null;

        try {
            AiMentService.MentResult ment = aiMentService.generateMent(emotionName);
            header = ment.header();
            footer = ment.footer();
        } catch (Exception e) {
            log.warn("[Gemini 멘트 생성 실패] curationId={}, cause={}", curationId, e.getMessage());
        }

        if (header == null || footer == null) {
            List<CurationMent> mentList = curationMentRepository.findAllByEmotion_EmotionId(emotionId);
            if (!mentList.isEmpty()) {
                CurationMent fallback = mentList.get(ThreadLocalRandom.current().nextInt(mentList.size()));
                header = fallback.getHeaderText();
                footer = fallback.getFooterText();
            }
        }

        if (header != null && footer != null) {
            curation.updateMent(
                    header.replace("(닉네임)", nickname),
                    footer.replace("(닉네임)", nickname)
            );
        }
    }
}
