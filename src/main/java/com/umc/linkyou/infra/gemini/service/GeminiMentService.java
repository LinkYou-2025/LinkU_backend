package com.umc.linkyou.infra.gemini.service;

import com.umc.linkyou.infra.ai.AiMentService;
import com.umc.linkyou.infra.ai.dto.MentResultDTO;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.prompt.curation.CurationMentPrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiMentService implements AiMentService {

    private final GeminiService geminiService;
    private final PromptComposer promptComposer;

    @Override
    public MentResultDTO generateMent(String emotionName) {
        return geminiService.callAndParseCreative(
                promptComposer.mention(),
                new CurationMentPrompt(emotionName).render(),
                MentResultDTO.class
        );
    }
}