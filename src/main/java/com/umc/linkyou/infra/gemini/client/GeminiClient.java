package com.umc.linkyou.infra.gemini.client;

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
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final Client client;

    @Value("${gemini.model.name}")
    private String modelName;

    private static final Tool GOOGLE_SEARCH_TOOL = Tool.builder()
            .googleSearch(GoogleSearch.builder().build())
            .build();

    public String completion(String systemInstruction, String userPrompt) {
        return generate(systemInstruction, userPrompt, null, 1024, 0.3f);
    }

    public String completionWithSearch(String systemInstruction, String userPrompt) {
        return generate(systemInstruction, userPrompt, GOOGLE_SEARCH_TOOL, 2048, 0.9f);
    }

    private String generate(String systemInstruction, String userPrompt, Tool tool, int maxTokens, float temp) {
        try {
            GenerateContentConfig.Builder builder = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                    .maxOutputTokens(maxTokens)
                    .temperature(temp);

            if (tool != null) {
                builder.tools(Collections.singletonList(tool));
            }

            return client.models.generateContent(modelName, userPrompt, builder.build()).text();
        } catch (Exception e) {
            if (e instanceof TimeoutException || e.getCause() instanceof TimeoutException) {
                throw new GeneralException(GeminiErrorStatus.GEMINI_TIMEOUT);
            }
            log.error("Gemini API 호출 오류", e);
            throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
        }
    }
}
