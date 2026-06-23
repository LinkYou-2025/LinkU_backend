package com.umc.linkyou.infra.gemini.service;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.infra.ai.AiLinkuAnalyzer;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.prompt.linku.CategoryClassifyPrompt;
import com.umc.linkyou.infra.parser.TitleDomainParser;
import com.umc.linkyou.infra.parser.WebContentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiLinkuService implements AiLinkuAnalyzer {

    private final TitleDomainParser titleDomainParser;
    private final WebContentExtractor webContentExtractor;
    private final GeminiService geminiService;
    private final PromptComposer promptComposer;

    @Override
    public Optional<LinkuResultDTO> analyzeByUrl(String url,Long jobId,  List<Category> categories, List<Situation> situations, List<Emotion> emotions) {
        // situations, emotions를 Gemini 프롬프트에 주입 → AI가 DB에 존재하는 ID로 situationId/emotionId 반환
        TitleDomainParser.ParsedPageInfo pageInfo = titleDomainParser.parseUrl(url);
        String domain = pageInfo.domain();
        String title = pageInfo.title();
        String pageContent = null;

        try {
            pageContent = webContentExtractor.extractTextFromUrl(url);
        } catch (Exception e) {
            log.warn("[본문 추출 실패] {}", e.getMessage());
        }

        if ((title == null || title.isBlank()) && (pageContent == null || pageContent.isBlank())) {
            log.warn("[카테고리 분류 실패] 제목, 본문 없음 → {}", url);
            return Optional.empty();
        }

        if (pageContent != null && pageContent.length() > 2000)
            pageContent = pageContent.substring(0, 2000);

        String categoryList = categories.stream()
                .map(c -> "- id: " + c.getCategoryId() + ", name: \"" + c.getCategoryName() + "\"")
                .collect(Collectors.joining("\n"));

        String situationList = situations.stream()
                .map(s -> "- id: " + s.getId() + ", name: \"" + s.getName() + "\"")
                .collect(Collectors.joining("\n"));

        String emotionList = emotions.stream()
                .map(e -> "- id: " + e.getEmotionId() + ", name: \"" + e.getName() + "\"")
                .collect(Collectors.joining("\n"));

        CategoryClassifyPrompt prompt = new CategoryClassifyPrompt(jobId,domain, title, pageContent, categoryList, situationList, emotionList);

        return Optional.of(
                geminiService.callAndParse(
                        promptComposer.general(),
                        prompt.render(),
                        LinkuResultDTO.class)
        );
    }
}
