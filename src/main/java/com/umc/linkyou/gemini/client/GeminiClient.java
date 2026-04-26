package com.umc.linkyou.gemini.client;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.Part;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final Client client; // Google SDK Client

    @Value("${gemini.model.name}")
    private String modelName;

    /**
     * Gemini API에 직접 요청을 보냅니다.
     * @param systemInstruction 시스템 지시문 (페르소나)
     * @param userPrompt 사용자 요청 (Task Instruction)
     * @return Gemini가 생성한 원본 텍스트
     */
    public String completion(String systemInstruction, String userPrompt) {
        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                    .maxOutputTokens(1024)
                    .temperature(0.3f)
                    .build();

            return client.models.generateContent(modelName, userPrompt, config).text();
        } catch (Exception e) {
            log.error("[Gemini API 호출 에러] model: {}, error: {}", modelName, e.getMessage());
            return null;
        }
    }
}
