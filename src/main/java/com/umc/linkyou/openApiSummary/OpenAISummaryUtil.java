package com.umc.linkyou.openApiSummary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.aiCategoryClassifier.util.WebContentExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Component
public class OpenAISummaryUtil {

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    private final ObjectMapper objectMapper;
    private final WebContentExtractor webContentExtractor;

    public SummaryAnalysisResultDTO getFullAnalysis(
            String url,
            List<?> situations,
            List<?> emotions,
            List<?> categories
    ) throws IOException {

        // 1. 본문 추출
        String pageContent = webContentExtractor.extractTextFromUrl(url);
        if (pageContent == null || pageContent.isBlank())
            throw new IOException("웹페이지 본문 추출 실패");

        if (pageContent.length() > 2800)
            pageContent = pageContent.substring(0, 2800);

        // 2. 프롬프트용 리스트 변환
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

        // 3. 프롬프트 생성
        String prompt = String.format("""
            다음 웹페이지 본문을 기반으로 다음 항목을 모두 생성해 주세요.
            
            📄 본문:
            %s

            🔸 제공된 목록에서 하나씩 선택하여 ID만 사용해 주세요.

            상황 (Situation):
            %s

            감정 (Emotion):
            %s

            카테고리 (Category):
            %s

            ✅ 응답 형식(JSON):
            {
              "title": "...",
              "summary": "...",
              "situationId": 7,
              "emotionId": 5,
              "categoryId": 2,
              "keywords": "#키워드1, #키워드2, ..."
            }

            ⚠ JSON 외 텍스트 없이 출력하세요. 설명이나 해설 금지.
            ⚠ 모든 응답은 한국어로 자연스럽게 작성해 주세요.
            """, pageContent, situationList, emotionList, categoryList);

        // 4. OpenAI 요청
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "당신은 웹 콘텐츠를 요약하고 분류하는 AI입니다."),
                        Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.7
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(requestBody), headers);

        log.info("[OpenAI 요청 시작]");
        log.debug("[OpenAI 프롬프트]:\n{}", prompt);

        ResponseEntity<JsonNode> response = new RestTemplate()
                .postForEntity(apiUrl, request, JsonNode.class);

        String rawContent = response.getBody()
                .path("choices").get(0)
                .path("message").path("content")
                .asText();

        log.info("[OpenAI 원본 응답 Content]:\n{}", rawContent);

        // 5. 마크다운 및 쓰레기 제거
        String sanitized = sanitizeOpenAiJson(rawContent);
        log.info("[정제된 응답 JSON]:\n{}", sanitized);

        // 6. 파싱
        try {
            return objectMapper.readValue(sanitized, SummaryAnalysisResultDTO.class);
        } catch (Exception e) {
            log.error("[AI 응답 파싱 실패]: {}", sanitized, e);
            throw new GeneralException(ErrorStatus._AI_PARSE_ERROR);
        }
    }

    /**
     * OpenAI 응답에서 마크다운, 불필요한 줄 제거하고 유효한 JSON만 남긴다.
     */
    private String sanitizeOpenAiJson(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new GeneralException(ErrorStatus._AI_PARSE_ERROR);
        }

        String content = rawContent.trim();

        // ```json 또는 ```
        if (content.contains("```")) {
            // ``` 로 시작하는 전체 라인 제거
            content = content.replaceAll("(?m)^```.*$", "");
            content = content.trim();
        }


        // 첫 { 부터 마지막 } 까지만 추출
        int startIndex = content.indexOf("{");
        int endIndex = content.lastIndexOf("}");

        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            log.error("[sanitizeOpenAiJson] 유효 JSON 범위 찾을 수 없음. 원본: {}", rawContent);
            throw new GeneralException(ErrorStatus._AI_PARSE_ERROR);
        }

        return content.substring(startIndex, endIndex + 1).trim();
    }
}
