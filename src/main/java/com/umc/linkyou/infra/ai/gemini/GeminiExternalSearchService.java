package com.umc.linkyou.infra.ai.gemini;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GoogleSearch;
import com.google.genai.types.Part;
import com.google.genai.types.Tool;
import com.umc.linkyou.infra.ai.GeminiJsonUtils;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiExternalSearchService {

    private final Client geminiClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.model.name}")
    private String modelName;

    // 구글 검색 도구(Grounding) 설정
    private static final Tool GOOGLE_SEARCH_TOOL = Tool.builder()
            .googleSearch(GoogleSearch.builder().build())
            .build();

    // 외부 링크 추천 기능 메인 로직
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

        // 시스템 프롬프트 (규칙 정의)
        String systemInstruction = """
            You are a WEB SEARCH assistant for personalized content curation.

            AUDIENCE PROFILE:
            - Job (primary): %s
            - Gender: %s
            - Locale: Korea (KR), language: Korean

            TARGETING RULES (Very Important):
            - Optimize for the user's *job context*: tasks, tools, workflows, skill growth, portfolio/career relevance.
            - Calibrate *difficulty*: beginner/intermediate/professional depending on common needs of the given job (prefer actionable and recent know-how).
            - Consider gender only to avoid unsafe/inappropriate content; DO NOT stereotype interests by gender.

            QUALITY / SAFETY RULES:
            - Return ONLY a JSON array, no prose/markdown/code fences.
            - Exactly %d items.
            - Each item: {"title":"...", "url":"..."} (both non-empty).
            - URL must be publicly reachable now (HTTP/HTTPS; no 404/401/502).
            - Prefer reputable Korean sources; avoid login/paywalls/spam/clickbait/aggregators.
            - Prefer content published/updated within the last 24 months unless clearly evergreen.
            - Exclude NSFW, gambling, high-risk financial advice, medical claims without reputable sources.
            - EXCLUDE content from these domains: [%s] (User already saw them).

            DIVERSITY & RELEVANCE:
            - Cover a *diverse set of domains* (avoid many results from the same site).
            - Maximize *topical relevance* to the user's tags and job. If a conflict, job relevance wins.
            - Titles should reflect practical value (guide, checklist, tutorial, case study, trend report).

            OUTPUT: JSON array only.
            """.formatted(safe(jobName), safe(gender), limit, excludedDomains);

        // 유저 프롬프트 (실제 요청)
        String userPrompt = """
            다음은 사용자가 최근 본 링크(절대 재사용 금지):
            %s

            사용자 중요 태그: %s

            요구사항:
            - 위 태그와 직무(%s)에 직결되는 주제 위주로, 실제 존재하는 공개 웹페이지를 정확히 %d개 추천.
            - 실무 적용 가능성 높은 콘텐츠(튜토리얼/체크리스트/가이드/트렌드 요약/사례연구) 선호.
            - 제목은 과장/낚시성 표현을 피하고 핵심 주제를 명확히 드러내는 자료만.

            형식: [{"title":"...","url":"..."}]
            """.formatted(
                String.join("\n", recentUrls),
                (tagNames == null || tagNames.isEmpty()) ? "(없음)" : String.join(", ", tagNames),
                safe(jobName),
                limit
        );

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                    .tools(GOOGLE_SEARCH_TOOL)
                    .maxOutputTokens(2048)
                    .temperature(0.9f)
                    .build();

            String rawResponse = geminiClient.models.generateContent(modelName, userPrompt, config).text();
            log.info("Gemini 응답: {}", rawResponse);

            // 마크다운 펜스 등 제거 후 배열 추출
            String jsonResponse = GeminiJsonUtils.extractJsonArray(rawResponse);
            if (jsonResponse == null) {
                log.warn("Gemini 응답에서 JSON 배열 추출 실패: {}", rawResponse);
                return Collections.emptyList();
            }

            // JSON 파싱
            List<Map<String, String>> parsed = objectMapper.readValue(jsonResponse, new TypeReference<>() {});

            // DTO 변환
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
