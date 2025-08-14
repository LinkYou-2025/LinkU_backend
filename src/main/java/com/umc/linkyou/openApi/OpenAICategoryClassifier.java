package com.umc.linkyou.openApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class OpenAICategoryClassifier {

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final ObjectMapper objectMapper;
    private final WebContentExtractor webContentExtractor;

    /**
     * URL 본문을 크롤링해서 카테고리 중 하나로 분류, 실패 시 null 반환
     */
    /**
     * URL 본문을 크롤링해서 카테고리 & 키워드 반환
     */
    public CategoryResult classifyCategoryByUrl(String url, List<?> categories) {
        try {
            String pageContent = webContentExtractor.extractTextFromUrl(url);
            if (pageContent == null || pageContent.isBlank()) {
                log.warn("[카테고리 분류 실패] 본문 추출 실패 URL: {}", url);
                return null;
            }

            if (pageContent.length() > 2800)
                pageContent = pageContent.substring(0, 2800);

            String categoryList = categories.stream()
                    .map(c -> {
                        var entity = (com.umc.linkyou.domain.classification.Category) c;
                        return "- id: " + entity.getCategoryId() + ", name: \"" + entity.getCategoryName() + "\"";
                    })
                    .collect(Collectors.joining("\n"));

            // 💡 여기서 categoryId + keywords를 같이 요청
            String prompt = String.format("""
                    다음 웹페이지 본문을 기반으로 제공된 카테고리 목록에서 하나를 선택하고, 
                    해당 웹페이지의 핵심 키워드 3~5개를 해시태그 형식으로 작성하세요.
                    
                    📄 본문:
                    %s
                    
                    카테고리 목록:
                    %s
                    
                    ✅ JSON 형식 예시:
                    {
                      "categoryId": 2,
                      "keywords": "#키워드1, #키워드2, #키워드3"
                    }
                    
                    ⚠ JSON 외 다른 내용 없이 출력하세요.
                    """, pageContent, categoryList);

            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "당신은 웹페이지 내용을 분석해 카테고리와 관련 키워드를 JSON으로만 반환하는 AI입니다."),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.3
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

            log.info("[OpenAI 요청 시작 - 카테고리+키워드 분류]");
            log.debug("[OpenAI 프롬프트]:\n{}", prompt);

            ResponseEntity<JsonNode> response = new RestTemplate().postForEntity(apiUrl, request, JsonNode.class);

            String rawContent = response.getBody()
                    .path("choices").get(0)
                    .path("message").path("content")
                    .asText();

            log.info("[OpenAI 카테고리+키워드 응답]: {}", rawContent);

            // JSON 추출
            int startIdx = rawContent.indexOf("{");
            int endIdx = rawContent.lastIndexOf("}");
            if (startIdx == -1 || endIdx == -1 || startIdx >= endIdx) {
                log.warn("[OpenAI 응답 파싱 실패] {}", rawContent);
                return null;
            }

            String jsonString = rawContent.substring(startIdx, endIdx + 1);
            return objectMapper.readValue(jsonString, CategoryResult.class);

        } catch (Exception e) {
            log.error("[카테고리+키워드 분류 에러]", e);
            return null;
        }
    }

    /**
     * 응답 DTO
     */
    public static class CategoryResult {
        private Long categoryId;
        private String keywords;

        public Long getCategoryId() {
            return categoryId;
        }

        public void setCategoryId(Long categoryId) {
            this.categoryId = categoryId;
        }

        public String getKeywords() {
            return keywords;
        }

        public void setKeywords(String keywords) {
            this.keywords = keywords;
        }
    }

}