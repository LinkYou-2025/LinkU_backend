package com.umc.linkyou.service.curation.gemini;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Gemini 텍스트 생성 전용 서비스 (Google Search 그라운딩 없음)
// 멘트 생성, 요약·분류, 카테고리 분류 등 입력 텍스트 기반 생성에 사용
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiTextService {

    private final Client geminiClient;

    @Value("${gemini.model.name}")
    private String modelName;

    /**
     * @param systemInstruction AI 역할 지시문
     * @param userPrompt 실제 요청 프롬프트
     * @return 생성된 텍스트, 실패 시 null
     */
    public String generateText(String systemInstruction, String userPrompt) {
        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                    .maxOutputTokens(1024)
                    .temperature(0.3f)
                    .build();

            return geminiClient.models.generateContent(modelName, userPrompt, config).text();
        } catch (Exception e) {
            log.error("GeminiTextService 텍스트 생성 오류", e);
            return null;
        }
    }
}
