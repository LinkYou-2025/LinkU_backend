package com.umc.linkyou.infra.ai;

/**
 *  공통 AI API 연결 클라이언트
 */
public interface AiClient {
    String completion(String systemInstruction, String userPrompt);

    String completionCreative(String systemInstruction, String userPrompt);

    String completionWithSearch(String systemInstruction, String userPrompt);
}
