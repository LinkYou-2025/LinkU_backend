package com.umc.linkyou.apiPayload.code.status.auth;


import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorStatus implements BaseErrorCode {
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH4001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "AUTH4001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.BAD_REQUEST, "AUTH4002", "만료된 토큰입니다."),
    UNSUPPORTED_TOKEN(HttpStatus.BAD_REQUEST, "AUTH4003", "지원되지 않는 토큰입니다."),
    INVALID_TOKEN_SIGNATURE(HttpStatus.BAD_REQUEST, "AUTH4004", "토큰 서명이 유효하지 않습니다."),
    TOKEN_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH5001", "토큰 파싱에 실패했습니다."),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "AUTH4005", "토큰이 만료되었습니다."),
    TOKEN_NOT_FOUND(HttpStatus.BAD_REQUEST, "AUTH4006", "토큰이 존재하지 않습니다."),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "AUTH4003", "권한이 없습니다."),
    _REFRESH_TOKEN_SESSION_SAVE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH5002", "세션 저장 중 오류가 발생했습니다.");

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
