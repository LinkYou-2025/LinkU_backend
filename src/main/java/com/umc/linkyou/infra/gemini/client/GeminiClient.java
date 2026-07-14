package com.umc.linkyou.infra.gemini.client;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.umc.linkyou.apiPayload.code.status.gemini.GeminiErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiClient {

    private final ObjectProvider<Client> clientProvider;

    @Value("${gemini.model.name}")
    private String modelName;

    private static final Tool GOOGLE_SEARCH_TOOL = Tool.builder()
            .googleSearch(GoogleSearch.builder().build())
            .build();

    public String completion(String systemInstruction, String userPrompt)
    {
        return generate(systemInstruction, userPrompt, null, 1024, 0.3f);
    }

    // 창의적인 응답 생성
    public String completionCreative(String systemInstruction, String userPrompt)
    {
        return generate(systemInstruction, userPrompt, null, 1024, 0.9f);
    }

    // Google Search로 실시간 정보 반영
    public String completionWithSearch(String systemInstruction, String userPrompt)
    {
        return generate(systemInstruction, userPrompt, GOOGLE_SEARCH_TOOL, 2048, 0.3f);
    }

    private String generate(String systemInstruction, String userPrompt, Tool tool, int maxTokens, float temp)
    {
        Client client = clientProvider.getIfAvailable();
        if (client == null) {
            log.warn("Gemini Client 비활성 상태에서 호출됨");
            throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
        }
        try {
            GenerateContentConfig.Builder builder = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                    .maxOutputTokens(maxTokens)
                    .temperature(temp);

            if (tool != null) {
                builder.tools(Collections.singletonList(tool));
            }

            return client.models.generateContent(modelName, userPrompt, builder.build()).text();
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            if (e.getCause() instanceof TimeoutException) {
                throw new GeneralException(GeminiErrorStatus.GEMINI_TIMEOUT);
            }
            log.error("Gemini API 호출 오류", e);
            throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
        }
    }
}
