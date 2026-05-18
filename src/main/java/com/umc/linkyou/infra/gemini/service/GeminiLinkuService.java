package com.umc.linkyou.infra.gemini.service;

import com.umc.linkyou.infra.ai.AiLinkuAnalyzer;
import com.umc.linkyou.infra.ai.dto.CategoryResultDTO;
import com.umc.linkyou.infra.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.infra.gemini.prompt.linku.CategoryClassifyPrompt;
import com.umc.linkyou.infra.parser.TitleDomainParser;
import com.umc.linkyou.infra.parser.WebContentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
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
    public CategoryResultDTO classifyCategoryByUrl(String url, List<?> categories) {
        try {
            TitleDomainParser.ParsedPageInfo pageInfo = titleDomainParser.parseUrl(url);
            String domain = pageInfo.domain();
            String title = pageInfo.title();
            String pageContent = null;

            try {
                pageContent = webContentExtractor.extractTextFromUrl(url);
            } catch (Exception e) {
                log.warn("[본문 추출 실패] {}", e.getMessage());
            }

            if ((domain == null || domain.isBlank()) &&
                    (title == null || title.isBlank()) &&
                    (pageContent == null || pageContent.isBlank())) {
                log.warn("[카테고리 분류 실패] URL에서 정보 없음 → {}", url);
                return null;
            }

            // 제목이 없으면 AI 호출 생략, 카테고리 기타(16) 고정
            if (title == null || title.isBlank()) {
                String fallbackTitle = (domain != null && !domain.isBlank()) ? domain : "제목 없음";
                log.info("[제목 없음] 도메인명으로 대체, AI 분류 호출 생략 → URL: {}", url);
                CategoryResultDTO fallback = new CategoryResultDTO();
                fallback.setCategoryId(16L);
                fallback.setKeywords(fallbackTitle);
                return fallback;
            }

            if (pageContent != null && pageContent.length() > 2000) {
                pageContent = pageContent.substring(0, 2000);
            }

            String categoryList = categories.stream()
                    .map(c -> {
                        var entity = (com.umc.linkyou.domain.classification.Category) c;
                        return "- id: " + entity.getCategoryId() + ", name: \"" + entity.getCategoryName() + "\"";
                    })
                    .collect(Collectors.joining("\n"));

            CategoryClassifyPrompt prompt = new CategoryClassifyPrompt(
                    domain != null ? domain : "없음",
                    title,
                    pageContent != null ? pageContent : "본문 없음",
                    categoryList
            );

            return geminiService.callAndParse(
                    promptComposer.general(),
                    prompt.render(),
                    CategoryResultDTO.class
            );

        } catch (Exception e) {
            log.error("[카테고리+키워드 분류 에러]", e);
            return null;
        }
    }
}
