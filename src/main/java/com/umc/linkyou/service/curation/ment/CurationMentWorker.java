package com.umc.linkyou.service.curation.ment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.domain.Curation;
import com.umc.linkyou.domain.classification.CurationMent;
import com.umc.linkyou.domain.enums.KeywordType;
import com.umc.linkyou.infra.ai.GeminiJsonUtils;
import com.umc.linkyou.infra.ai.gemini.GeminiTextService;
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
import java.util.Map;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurationMentWorker {

    private final CurationRepository curationRepository;
    private final KeywordMonthlyCountRepository keywordMonthlyCountRepository;
    private final EmotionTagMapper emotionTagMapper;
    private final GeminiTextService geminiTextService;
    private final CurationMentRepository curationMentRepository;
    private final ObjectMapper objectMapper;

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
            String userPrompt = String.format(
                    "사용자의 현재 감정은 '%s'입니다.\n" +
                            "이 감정을 기반으로, 해당 사용자에게 맞는 콘텐츠를 소개하는 큐레이션 멘트를 작성해주세요.\n\n" +
                            "[큐레이션 멘트 설명]\n" +
                            "- 상단 멘트는 큐레이션 페이지 가장 위에 노출되어, 사용자의 감정에 공감하며 관심을 끌어야 합니다.\n" +
                            "- 하단 멘트는 큐레이션 페이지 마지막에 노출되며, 콘텐츠를 마무리하면서 위로나 응원을 담아야 합니다.\n\n" +
                            "[작성 규칙]\n" +
                            "- 각 멘트는 반드시 한 문장으로 작성하세요.\n" +
                            "- 반드시 \"(닉네임)\"이라는 텍스트를 포함하세요. 이 표현은 절대로 바꾸지 마세요.\n" +
                            "- 아래 형식의 JSON 형태로만 출력하세요:\n" +
                            "{\n" +
                            "  \"header\": \"(닉네임)님, ...\",\n" +
                            "  \"footer\": \"(닉네임)님, ...\"\n" +
                            "}\n\n" +
                            "※ JSON 외에는 아무것도 출력하지 말고, (닉네임)이라는 문자열을 절대로 수정하지 마세요.",
                    emotionName
            );

            String rawJson = geminiTextService.generateText(
                    "당신은 감정 기반 콘텐츠 추천 서비스의 큐레이션 에디터입니다.",
                    userPrompt
            );

            if (rawJson != null) {
                String cleaned = GeminiJsonUtils.extractJson(rawJson);
                if (cleaned != null) {
                    Map<String, String> ment = objectMapper.readValue(cleaned, Map.class);
                    header = ment.get("header");
                    footer = ment.get("footer");
                }
            }
        } catch (Exception e) {
            log.warn("[Gemini 멘트 생성 실패] curationId={}, cause={}", curationId, e.getMessage());
        }

        if (header == null || footer == null) {
            List<CurationMent> mentList = curationMentRepository.findAllByEmotion_EmotionId(emotionId);
            if (!mentList.isEmpty()) {
                CurationMent fallback = mentList.get(new Random().nextInt(mentList.size()));
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
