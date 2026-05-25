package com.umc.linkyou.infra.gemini.service;

import com.umc.linkyou.apiPayload.code.status.aiarticle.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.infra.ai.AiArticleAnalyzer;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.prompt.linku.LinkSummaryPrompt;
import com.umc.linkyou.infra.parser.WebContentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiArticleService implements AiArticleAnalyzer {

    private final WebContentExtractor webContentExtractor;
    private final GeminiService geminiService;
    private final PromptComposer promptComposer;

    @Override
    public AiArticleResultDTO analyzeByUrl(
            String url,
            List<Situation> situations,
            List<Emotion> emotions
    ) {
        String pageContent = webContentExtractor.extractTextFromUrl(url);
        if (pageContent == null || pageContent.isBlank())
            throw new GeneralException(AiArticleErrorStatus._CONTENT_EXTRACTION_FAILED);

        if (pageContent.length() > 2800)
            pageContent = pageContent.substring(0, 2800);

        String situationList = situations.stream()
                .map(s -> "- id: " + s.getId() + ", name: \"" + s.getName() + "\"")
                .collect(Collectors.joining("\n"));

        String emotionList = emotions.stream()
                .map(e -> "- id: " + e.getEmotionId() + ", name: \"" + e.getName() + "\"")
                .collect(Collectors.joining("\n"));

        LinkSummaryPrompt prompt = new LinkSummaryPrompt(pageContent, situationList, emotionList);

        return geminiService.callAndParse(
                promptComposer.general(),
                prompt.render(),
                AiArticleResultDTO.class
        );
    }
}
