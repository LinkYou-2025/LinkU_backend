package com.umc.linkyou.apiPayload.code.status.gemini;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GeminiErrorStatus implements BaseErrorCode {

    GEMINI_BAD_REQUEST(HttpStatus.BAD_REQUEST, "GEMINI4001", "잘못된 AI 요청입니다."),
    GEMINI_TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "GEMINI4291", "AI 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    GEMINI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "GEMINI5041", "Gemini 응답 시간이 초과되었습니다."),
    GEMINI_API_ERROR(HttpStatus.BAD_GATEWAY, "GEMINI5021", "Gemini API 호출 중 오류가 발생했습니다."),
    GEMINI_PARSE_ERROR(HttpStatus.BAD_GATEWAY, "GEMINI5022", "AI 응답 JSON 파싱에 실패했습니다."),
    GEMINI_UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GEMINI5001", "AI 처리 중 알 수 없는 오류가 발생했습니다."),
    GEMINI_RESPONSE_FORMAT_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GEMINI5002", "AI 응답 형식이 올바르지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder()
                .message(message)
                .code(code)
                .isSuccess(false)
                .httpStatus(httpStatus)
                .build();
    }
}
