package com.umc.linkyou.apiPayload.code.status.linku;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;


@Getter
@AllArgsConstructor
public enum LinkuErrorStatus implements BaseErrorCode {
    //링큐 관련 코드
    _LINKU_VIDEO_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "LINKU4001", "영상 링크는 저장할 수 없습니다."),
    _LINKU_INVALID_URL(HttpStatus.BAD_REQUEST, "LINKU4002", "유효하지 않은 링크입니다."),
    _USER_LINKU_NOT_FOUND(HttpStatus.NOT_FOUND, "LINKU404", "user_linku 테이블을 찾기 못했습니다." ),
    _LINKU_NOT_FOUND(HttpStatus.NOT_FOUND, "LINKU4041", "해당 링크 정보를 찾을 수 없습니다."),
    _LINKU_SEARCH_KEYWORD_REQUIRED(HttpStatus.BAD_REQUEST, "LINKU4003", "검색어는 비어 있을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public ErrorReasonDTO getReason() {
        return ErrorReasonDTO.builder().message(message).code(code).isSuccess(false).build();
    }

    @Override
    public ErrorReasonDTO getReasonHttpStatus() {
        return ErrorReasonDTO.builder().message(message).code(code).isSuccess(false).httpStatus(httpStatus).build();
    }
}
