package com.umc.linkyou.infra.gemini.service;

import com.umc.linkyou.infra.ai.AiMentService;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.prompt.curation.CurationMentPrompt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiMentService implements AiMentService {

    private final GeminiService geminiService;
    private final PromptComposer promptComposer;

    private record MentJson(String header, String footer) {}

    @Override
    public MentResult generateMent(String emotionName) {
        MentJson result = geminiService.callAndParse(
                promptComposer.mention(),
                new CurationMentPrompt(emotionName).render(),
                MentJson.class
        );
        return new MentResult(result.header(), result.footer());
    }
}
