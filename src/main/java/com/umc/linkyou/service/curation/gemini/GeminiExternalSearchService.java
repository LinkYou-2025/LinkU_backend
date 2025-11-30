package com.umc.linkyou.service.curation.gemini;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.api.GenerationConfig;
import com.google.cloud.vertexai.api.Tool;
import com.google.cloud.vertexai.api.GoogleSearchRetrieval;
import com.google.cloud.vertexai.generativeai.ContentMaker;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.google.cloud.vertexai.generativeai.ResponseHandler;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiExternalSearchService {

    private final ObjectMapper objectMapper;

    // application.properties에서 값 가져오기
    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.location}")
    private String location;

    @Value("${gemini.model.name}")
    private String modelName;

    private VertexAI vertexAI;
    private GenerativeModel model;

    /**
     * 서버 시작 시 Gemini 모델과 연결 설정 (Client 역할 대체)
     */
    @PostConstruct
    public void init() {
        try {
            // 1. Vertex AI 클라이언트 초기화
            // (참고: 로컬 개발 시 환경변수 GOOGLE_APPLICATION_CREDENTIALS 설정 필수)
            this.vertexAI = new VertexAI(projectId, location);

            // 2. 구글 검색 도구(Grounding) 설정
            Tool googleSearchTool = Tool.newBuilder()
                    .setGoogleSearchRetrieval(
                            GoogleSearchRetrieval.newBuilder().build()
                    )
                    .build();

            // 3. 생성 설정 (JSON 포맷 강제 등)
            GenerationConfig generationConfig = GenerationConfig.newBuilder()
                    .setMaxOutputTokens(2048)
                    .setTemperature(0.9f) // 창의성(다양한 검색 결과)을 위해 높임
                    .setResponseMimeType("application/json") // JSON 응답 강제
                    .build();

            // 4. 모델 생성
            this.model = new GenerativeModel.Builder()
                    .setModelName(modelName)
                    .setVertexAi(vertexAI)
                    .setTools(Collections.singletonList(googleSearchTool))
                    .setGenerationConfig(generationConfig)
                    .build();

            log.info("✅ Gemini Search Service 초기화 완료 (Project: {}, Location: {})", projectId, location);

        } catch (Exception e) {
            log.error("❌ Gemini 초기화 실패: GCP 인증 파일이나 설정을 확인하세요.", e);
        }
    }

    /**
     * 서버 종료 시 리소스 정리
     */
    @PreDestroy
    public void close() {
        if (this.vertexAI != null) {
            this.vertexAI.close();
        }
    }

    /**
     * 외부 링크 추천 기능 메인 로직
     */
    public List<RecommendedLinkResponse> searchExternalLinks(
            List<String> recentUrls,
            List<String> tagNames,
            int limit,
            String jobName,
            String gender
    ) {
        // 중복 방지를 위해 이미 본 URL에서 도메인만 추출 (예: naver.com, tistory.com)
        String excludedDomains = recentUrls.stream()
                .map(this::extractDomain)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", "));

        // 1. 시스템 프롬프트 (규칙 정의)
        String systemInstruction = """
            You are a professional content curator for '%s'.
            Target Audience Job: %s
            
            [CRITICAL RULES]
            1. Use Google Search to find REAL, LIVE web pages.
            2. EXCLUDE content from these domains: [%s] (User already saw them).
            3. Find NEW content (Published within the last 1 year preferred).
            4. Output must be a pure JSON Array.
            5. Fields: "title", "url", "summary".
            """.formatted(safe(jobName), safe(jobName), excludedDomains);

        // 2. 유저 프롬프트 (실제 요청)
        String userPrompt = """
            Find %d high-quality, practical links about: %s.
            Focus on tutorials, trends, or engineering blogs.
            Exclude generic wikis.
            """.formatted(limit, String.join(", ", tagNames));

        try {
            // 3. Gemini에게 질문 (여기가 Client.chat() 역할)
            GenerateContentResponse response = model.generateContent(
                    ContentMaker.fromMultiModalData(systemInstruction + "\n\n" + userPrompt)
            );

            // 4. 응답 텍스트 추출
            String jsonResponse = ResponseHandler.getText(response);
            log.info("Gemini 응답: {}", jsonResponse);

            // 5. JSON 파싱
            List<Map<String, String>> parsed = objectMapper.readValue(jsonResponse, new TypeReference<>() {});

            // 6. DTO 변환
            return parsed.stream()
                    .filter(m -> m.get("url") != null && !m.get("url").isBlank())
                    .limit(limit)
                    .map(m -> RecommendedLinkResponse.builder()
                            .title(m.getOrDefault("title", "No Title"))
                            .url(m.get("url"))
                            .domain(extractDomain(m.get("url")))
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Gemini 검색 중 오류 발생", e);
            return Collections.emptyList();
        }
    }

    // null 방지용
    private String safe(String s) {
        return (s == null || s.isBlank()) ? "Technology" : s;
    }

    // URL에서 도메인 추출 (예: https://www.naver.com/news -> naver.com)
    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain != null && domain.startsWith("www.")) {
                return domain.substring(4);
            }
            return domain;
        } catch (Exception e) {
            return null;
        }
    }
}