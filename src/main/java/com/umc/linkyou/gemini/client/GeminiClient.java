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
            if (e.getCause() instanceof TimeoutException || e instanceof TimeoutException) {
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
            // [해결] 명시적인 Builder 타입을 쓰지 않고 바로 호출 체이닝을 사용하거나,
            // 변수에 담으려면 GenerateContentConfig.Builder (내부 클래스)를 사용해야 합니다.
            GenerateContentConfig.Builder builder = GenerateContentConfig.builder()
                    .systemInstruction(Content.fromParts(Part.fromText(systemInstruction)))
                    .maxOutputTokens(maxTokens)
                    .temperature(temp);

            if (tool != null) {
                // [해결] tools 메서드 인자 형식을 확인하세요.
                // 보통 List<Tool>을 받습니다.
                builder.tools(Collections.singletonList(tool));
            }

            GenerateContentConfig config = builder.build();
            return client.models.generateContent(modelName, userPrompt, config).text();

        } catch (Exception e) {
            log.error("[Gemini API 에러] {}", e.getMessage());
            // [체크] GeminiErrorCode에 GEMINI_TIMEOUT이 정의되어 있어야 합니다.
            if (e.getCause() instanceof TimeoutException) {
                throw new GeneralException(GeminiErrorStatus.GEMINI_TIMEOUT);
            }
            throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
        }
    }

}
