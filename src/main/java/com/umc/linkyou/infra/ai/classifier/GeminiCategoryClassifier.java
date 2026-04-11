package com.umc.linkyou.infra.ai.classifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.infra.ai.GeminiJsonUtils;
import com.umc.linkyou.infra.parser.TitleDomainParser;
import com.umc.linkyou.infra.parser.WebContentExtractor;
import com.umc.linkyou.service.curation.gemini.GeminiTextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class GeminiCategoryClassifier {

    private final ObjectMapper objectMapper;
    private final TitleDomainParser titleDomainParser;
    private final WebContentExtractor webContentExtractor;
    private final GeminiTextService geminiTextService;

    public CategoryResult classifyCategoryByUrl(String url, List<?> categories) {
        try {
            String domain = null;
            String title = null;
            String pageContent = null;

            TitleDomainParser.ParsedPageInfo pageInfo = titleDomainParser.parseUrl(url);
            domain = pageInfo.domain();
            title = pageInfo.title();

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

            // 제목이 없으면 도메인명을 제목으로 대체하고, 카테고리는 기타(16)로 고정
            if (title == null || title.isBlank()) {
                String fallbackTitle = (domain != null && !domain.isBlank()) ? domain : "제목 없음";
                log.info("[제목 없음] 도메인명으로 대체, AI 분류 호출 생략 → URL: {}", url);
                CategoryResult fallbackResult = new CategoryResult();
                fallbackResult.setCategoryId(16L);
                fallbackResult.setKeywords(fallbackTitle);
                return fallbackResult;
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

            String prompt = String.format("""
                다음은 특정 URL에서 가져온 정보입니다.

                🌐 도메인: %s
                📝 제목: %s
                📄 본문(일부): %s

                위 정보를 기반으로 아래 카테고리 목록 중 하나를 선택하고,
                해당 웹페이지의 핵심 키워드 3~5개를 해시태그 형식(#)으로 작성하세요.
                본문보다 도메인, 제목이 연관 가능성이 높습니다.
                각 정보 중 null인 것은 참고하지 마십시오.

                카테고리 목록:
                %s

                ✅ JSON 형식 예시:
                {
                  "categoryId": 2,
                  "keywords": "#키워드1, #키워드2, #키워드3"
                }

                ⚠ JSON 외 다른 내용 없이 출력하세요.
                """,
                    domain != null ? domain : "없음",
                    title != null && !title.isBlank() ? title : domain != null ? domain : "없음",
                    pageContent != null ? pageContent : "본문 없음",
                    categoryList);

            String rawContent = geminiTextService.generateText(
                    "당신은 URL의 도메인/제목/본문을 분석해 카테고리와 핵심 키워드를 JSON 형식으로 반환하는 AI입니다.",
                    prompt
            );

            String jsonString = GeminiJsonUtils.extractJson(rawContent);
            if (jsonString == null) {
                log.warn("[Gemini 응답 파싱 실패] {}", rawContent);
                return null;
            }

            return objectMapper.readValue(jsonString, CategoryResult.class);

        } catch (Exception e) {
            log.error("[카테고리+키워드 분류 에러]", e);
            return null;
        }
    }

    public static class CategoryResult {
        private Long categoryId;
        private String keywords;

        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public String getKeywords() { return keywords; }
        public void setKeywords(String keywords) { this.keywords = keywords; }
    }
}
