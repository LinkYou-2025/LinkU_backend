package com.umc.linkyou.infra.gemini.service;

import com.umc.linkyou.infra.ai.AiArticleAnalyzer;
import com.umc.linkyou.infra.ai.dto.AiArticleResultDTO;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.prompt.linku.LinkSummaryPrompt;
import com.umc.linkyou.infra.parser.WebContentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    public AiArticleResultDTO getFullAnalysis(
            String url,
            List<?> situations,
            List<?> emotions,
            List<?> categories
    ) throws IOException {

        String pageContent = webContentExtractor.extractTextFromUrl(url);
        if (pageContent == null || pageContent.isBlank())
            throw new IOException("웹페이지 본문 추출 실패");

        if (pageContent.length() > 2800)
            pageContent = pageContent.substring(0, 2800);

        String situationList = situations.stream()
                .map(s -> {
                    var entity = (com.umc.linkyou.domain.classification.Situation) s;
                    return "- id: " + entity.getId() + ", name: \"" + entity.getName() + "\"";
                })
                .collect(Collectors.joining("\n"));

        String emotionList = emotions.stream()
                .map(e -> {
                    var entity = (com.umc.linkyou.domain.classification.Emotion) e;
                    return "- id: " + entity.getEmotionId() + ", name: \"" + entity.getName() + "\"";
                })
                .collect(Collectors.joining("\n"));

        String categoryList = categories.stream()
                .map(c -> {
                    var entity = (com.umc.linkyou.domain.classification.Category) c;
                    return "- id: " + entity.getCategoryId() + ", name: \"" + entity.getCategoryName() + "\"";
                })
                .collect(Collectors.joining("\n"));

        LinkSummaryPrompt prompt = new LinkSummaryPrompt(pageContent, situationList, emotionList, categoryList);

        return geminiService.callAndParse(
                promptComposer.general(),
                prompt.render(),
                AiArticleResultDTO.class
        );
    }
}
