package com.umc.linkyou.gemini.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeminiErrorCode {
    GEMINI_API_ERROR(HttpStatus.BAD_GATEWAY, "GEMINI-001", "Gemini API 호출 중 오류가 발생했습니다."),
    GEMINI_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GEMINI-002", "AI 응답 파싱에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
