package com.umc.linkyou.gemini.client;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.umc.linkyou.apiPayload.code.status.gemini.GeminiErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component("geminiApiWrapper")
@RequiredArgsConstructor
public class GeminiClient {

    private final Client client; // Google SDK Client

    @Value("${gemini.model.name}")
    private String modelName;

    private static final Tool GOOGLE_SEARCH_TOOL = Tool.builder()
            .googleSearch(GoogleSearch.builder().build())
            .build();

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
            if (e instanceof TimeoutException || e.getCause() instanceof TimeoutException) {
                throw new GeneralException(GeminiErrorStatus.GEMINI_TIMEOUT);
            }
            throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
        }
    }
    /**
     * 구글 검색 기반 텍스트 생성 (기존 GeminiExternalSearchService 역할)
     */
    public String completionWithSearch(String systemInstruction, String userPrompt) {
        return generate(systemInstruction, userPrompt, GOOGLE_SEARCH_TOOL, 2048, 0.9f);
    }

    private String generate(String systemInstruction, String userPrompt, Tool tool, Integer maxTokens, Float temp) {
        try {
            GenerateContentConfig.Builder builder = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                    .maxOutputTokens(maxTokens)
                    .temperature(temp);

            if (tool != null) {
                builder.tools(Collections.singletonList(tool));
            }

            GenerateContentConfig config = builder.build();
            return client.models.generateContent(modelName, userPrompt, config).text();

        } catch (Exception e) {
            if (e.getCause() instanceof TimeoutException) {
                throw new GeneralException(GeminiErrorStatus.GEMINI_TIMEOUT);
            }
            throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
        }
    }

}
