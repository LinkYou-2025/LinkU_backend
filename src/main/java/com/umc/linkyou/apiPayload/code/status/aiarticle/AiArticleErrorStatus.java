package com.umc.linkyou.apiPayload.code.status.aiarticle;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiArticleErrorStatus implements BaseErrorCode {

    // 400 Bad Request
    _AI_PARSE_ERROR(HttpStatus.BAD_REQUEST, "OPENAI5001", "AI 응답 파싱에 실패했습니다."),

    // 404 Not Found
    _AI_ARTICLE_NOT_FOUND(HttpStatus.NOT_FOUND, "AIARTICLE4041", "해당하는 AI Article을 찾을 수 없습니다."),
    _CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY4041", "해당하는 카테고리를 찾을 수 없습니다."),
    _SITUATION_NOT_FOUND(HttpStatus.NOT_FOUND, "SITUATION4041", "해당하는 상황을 찾을 수 없습니다."),

    // 409 Conflict
    _DUPLICATE_AI_ARTICLE(HttpStatus.CONFLICT, "AIARTICLE4091", "이미 해당 링크로 생성된 AI Article이 존재합니다."),

    // 500 Internal Server Error
    _AI_INVALID_RESPONSE(HttpStatus.INTERNAL_SERVER_ERROR, "OPENAI5002", "AI 응답이 예상한 형식이 아닙니다."),
    _CONTENT_EXTRACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CRAWLER5001", "웹페이지 본문 추출에 실패했습니다."),
    _CONTENT_EXTRACTION_PROHIBITED(HttpStatus.INTERNAL_SERVER_ERROR, "CRAWLER5002", "크롤링이 금지된 웹사이트입니다.");

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
