package com.umc.linkyou.gemini.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.umc.linkyou.apiPayload.code.status.GeminiErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.gemini.client.GeminiClient;
import com.umc.linkyou.infra.ai.GeminiJsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiClient geminiApiClient;
    private final ObjectMapper objectMapper;

    // 객체 하나 파싱
    public <T> T callAndParse(String systemMsg, String userMsg, Class<T> clazz) {
        String rawContent = geminiApiClient.completion(systemMsg, userMsg);
        return parse(rawContent, clazz);
    }

    // 리스트(배열) 파싱 (검색 결과용)
    public <T> List<T> callAndParseList(String systemMsg, String userMsg, TypeReference<List<T>> typeReference) {
        String rawContent = geminiApiClient.completionWithSearch(systemMsg, userMsg); // 검색 모드 사용
        String sanitizedJson = GeminiJsonUtils.extractJsonArray(rawContent);

        try {
            return objectMapper.readValue(sanitizedJson, typeReference);
        } catch (Exception e) {
            throw new GeneralException(GeminiErrorStatus.GEMINI_PARSE_ERROR);
        }
    }

    private <T> T parse(String raw, Class<T> clazz) {
        String sanitized = GeminiJsonUtils.extractJson(raw);
        try {
            return objectMapper.readValue(sanitized, clazz);
        } catch (Exception e) {
            throw new GeneralException(GeminiErrorStatus.GEMINI_PARSE_ERROR);
        }
    }
}
