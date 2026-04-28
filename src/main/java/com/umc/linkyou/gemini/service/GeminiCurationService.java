package com.umc.linkyou.gemini.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.umc.linkyou.gemini.prompt.common.PromptComposer;
import com.umc.linkyou.gemini.prompt.curation.ExternalSearchPrompt;
import com.umc.linkyou.web.dto.curation.RecommendedLinkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiCurationService {

    private final GeminiService geminiService;
    private final PromptComposer promptComposer;

    /**
     * 외부 링크 추천 로직
     */
    public List<RecommendedLinkResponse> getExternalRecommendations(
            List<String> recentUrls,
            List<String> tags,
            int limit,
            String job,
            String gender
    ) {
        // 1. 중복 방지를 위한 도메인 제외 문자열 생성
        String excludedDomains = recentUrls.stream()
                .map(this::extractDomain)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(", "));

        // 2. 도메인 프롬프트(Task) 생성
        ExternalSearchPrompt task = new ExternalSearchPrompt(
                safe(job),
                safe(gender),
                limit,
                excludedDomains,
                String.join("\n", recentUrls),
                (tags == null || tags.isEmpty()) ? "(없음)" : String.join(", ", tags)
        );

        // 3. AI 호출 및 리스트 파싱 (GeminiService의 callAndParseList 활용)
        // TypeReference를 사용하여 List<Map<String, String>> 구조로 안전하게 가져옵니다.
        List<Map<String, String>> rawList = geminiService.callAndParseList(
                "당신은 구글 검색을 활용하는 웹 큐레이션 전문가입니다.",
                promptComposer.composeCuration(task), // curation 시스템 프롬프트
                new TypeReference<List<Map<String, String>>>() {}
        );

        // 4. RecommendedLinkResponse DTO로 변환
        return rawList.stream()
                .filter(m -> m.get("url") != null && !m.get("url").isBlank())
                .map(m -> RecommendedLinkResponse.builder()
                        .title(m.getOrDefault("title", "No Title"))
                        .url(m.get("url"))
                        .domain(extractDomain(m.get("url")))
                        .build())
                .collect(Collectors.toList());
    }

    // --- Utility Methods ---

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "Technology" : s;
    }

    private String extractDomain(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain != null && domain.startsWith("www.")) {
                return domain.substring(4);
            }
            return domain;
        } catch (Exception e) {
            log.warn("[도메인 추출 실패] url: {}", url);
            return null;
        }
    }
}
