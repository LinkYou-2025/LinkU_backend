package com.umc.linkyou.infra.gemini.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.gemini.GeminiErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.infra.gemini.GeminiJsonUtils;
import com.umc.linkyou.infra.gemini.client.GeminiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public <T> T callAndParse(String systemMsg, String userMsg, Class<T> clazz) {
        String raw = geminiClient.completion(systemMsg, userMsg);
        String sanitized = GeminiJsonUtils.extractJson(raw);
        if (sanitized == null) {
            throw new GeneralException(GeminiErrorStatus.GEMINI_PARSE_ERROR);
        }
        try {
            return objectMapper.readValue(sanitized, clazz);
        } catch (Exception e) {
            throw new GeneralException(GeminiErrorStatus.GEMINI_PARSE_ERROR);
        }
    }

    public <T> List<T> callAndParseList(String systemMsg, String userMsg, TypeReference<List<T>> typeRef) {
        String raw = geminiClient.completionWithSearch(systemMsg, userMsg);
        String sanitized = GeminiJsonUtils.extractJsonArray(raw);
        if (sanitized == null) {
            throw new GeneralException(GeminiErrorStatus.GEMINI_PARSE_ERROR);
        }
        try {
            return objectMapper.readValue(sanitized, typeRef);
        } catch (Exception e) {
            throw new GeneralException(GeminiErrorStatus.GEMINI_PARSE_ERROR);
        }
    }
}
