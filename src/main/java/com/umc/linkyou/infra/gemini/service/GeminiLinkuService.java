package com.umc.linkyou.infra.gemini.service;

import com.umc.linkyou.domain.classification.Category;
import com.umc.linkyou.domain.classification.Emotion;
import com.umc.linkyou.domain.classification.Situation;
import com.umc.linkyou.infra.ai.AiLinkuAnalyzer;
import com.umc.linkyou.infra.ai.dto.LinkuResultDTO;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.prompt.linku.CategoryClassifyPrompt;
import com.umc.linkyou.infra.parser.NaverBlogIframeFetcher;
import com.umc.linkyou.infra.parser.TitleDomainParser;
import com.umc.linkyou.infra.parser.WebContentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiLinkuService implements AiLinkuAnalyzer {

    private static final int MAX_PAGE_CONTENT_CHARS = 500;
    private static final int START_CONTENT_CHARS = 250;
    private static final int MIDDLE_CONTENT_CHARS = 125;
    private static final int END_CONTENT_CHARS = 125;

    private final TitleDomainParser titleDomainParser;
    private final WebContentExtractor webContentExtractor;
    private final GeminiService geminiService;
    private final PromptComposer promptComposer;

    public Optional<LinkuResultDTO> analyzeByUrl(String url, List<Category> categories, List<Situation> situations, List<Emotion> emotions) {
        return analyzeByUrl(url, null, categories, situations, emotions);
    }

    @Override
    public Optional<LinkuResultDTO> analyzeByUrl(String url, Document doc, List<Category> categories, List<Situation> situations, List<Emotion> emotions) {
        return analyzeByUrl(url, doc, categories, situations, emotions, null);
    }

    public Optional<LinkuResultDTO> analyzeByUrl(
            String url,
            Document doc,
            List<Category> categories,
            List<Situation> situations,
            List<Emotion> emotions,
            NaverBlogIframeFetcher.Result iframePage) {
        // situations, emotions를 Gemini 프롬프트에 주입 → AI가 DB에 존재하는 ID로 situationId/emotionId 반환
        TitleDomainParser.ParsedPageInfo pageInfo = (doc != null)
                ? titleDomainParser.parseUrl(url, doc)
                : titleDomainParser.parseUrl(url);
        String domain = pageInfo.domain();
        String title = pageInfo.title();
        String pageContent = null;

        long contentExtractionStartedAt = System.nanoTime();
        try {
            pageContent = (doc != null)
                    ? webContentExtractor.extractTextFromDocument(url, doc, iframePage)
                    : webContentExtractor.extractTextFromUrl(url);
        } catch (Exception e) {
            log.warn("[본문 추출 실패] {}", e.getMessage());
        } finally {
            log.info("[링크 생성 시간] 단계=content-extraction elapsedMs={} success={}",
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - contentExtractionStartedAt),
                    pageContent != null && !pageContent.isBlank());
        }

        if ((title == null || title.isBlank()) && (pageContent == null || pageContent.isBlank())) {
            log.warn("[카테고리 분류 실패] 제목, 본문 없음 → {}", url);
            return Optional.empty();
        }

        pageContent = selectRepresentativeContent(pageContent);

        String categoryList = categories.stream()
                .map(c -> "- id: " + c.getCategoryId() + ", name: \"" + c.getCategoryName() + "\"")
                .collect(Collectors.joining("\n"));

        String situationList = situations.stream()
                .map(s -> "- id: " + s.getId() + ", name: \"" + s.getName() + "\"")
                .collect(Collectors.joining("\n"));

        String emotionList = emotions.stream()
                .map(e -> "- id: " + e.getEmotionId() + ", name: \"" + e.getName() + "\"")
                .collect(Collectors.joining("\n"));

        CategoryClassifyPrompt prompt = new CategoryClassifyPrompt(domain, title, pageContent, categoryList, situationList, emotionList);

        return Optional.of(
                geminiService.callAndParse(
                        promptComposer.general(),
                        prompt.render(),
                        LinkuResultDTO.class)
        );
    }

    // 긴 본문은 앞·중간·끝 구간을 골라 최대 500자로 줄인다.
    private String selectRepresentativeContent(String content) {
        if (content == null || content.length() <= MAX_PAGE_CONTENT_CHARS) {
            return content;
        }

        int middleStart = Math.max(
                START_CONTENT_CHARS, (content.length() - MIDDLE_CONTENT_CHARS) / 2);
        int endStart = content.length() - END_CONTENT_CHARS;

        return "[앞부분]\n" + content.substring(0, START_CONTENT_CHARS).strip()
                + "\n[중간 부분]\n"
                + content.substring(middleStart, middleStart + MIDDLE_CONTENT_CHARS).strip()
                + "\n[끝부분]\n" + content.substring(endStart).strip();
    }
}
