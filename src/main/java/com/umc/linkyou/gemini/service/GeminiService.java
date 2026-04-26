package com.umc.linkyou.gemini.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.gemini.GeminiErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.gemini.client.GeminiClient;
import com.umc.linkyou.gemini.exception.GeminiErrorCode;
import com.umc.linkyou.infra.ai.GeminiJsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    /**
     * AI를 호출하고 그 결과를 특정 클래스 객체로 변환합니다.
     */
    public <T> T callAndParse(String systemMsg, String userMsg, Class<T> clazz) {
        // 1. Client를 통해 AI 호출
        String rawContent = geminiClient.completion(systemMsg, userMsg);

        if (rawContent == null || rawContent.isBlank()) {
            throw new GeneralException(GeminiErrorStatus.GEMINI_API_ERROR);
        }

        // 2. JSON 추출 유틸 사용
        String sanitizedJson = GeminiJsonUtils.extractJson(rawContent);
        if (sanitizedJson == null) {
            throw new GeneralException(GeminiErrorStatus.GEMINI_RESPONSE_FORMAT_ERROR);
        }

        // 3. ObjectMapping (파싱)
        try {
            return objectMapper.readValue(sanitizedJson, clazz);
        } catch (Exception e) {
            throw new GeneralException(GeminiErrorStatus.GEMINI_PARSE_ERROR);
        }
    }
}
