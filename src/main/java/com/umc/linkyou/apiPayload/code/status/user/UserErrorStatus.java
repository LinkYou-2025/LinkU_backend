package com.umc.linkyou.apiPayload.code.status.user;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorStatus implements BaseErrorCode {
    // 400 Bad Request
    INVALID_TERMS_TYPE(HttpStatus.BAD_REQUEST, "TERMS4001", "유효하지 않은 약관 타입입니다."),
    _INVALID_GENDER(HttpStatus.BAD_REQUEST, "USERS4002", "성별을 올바르게 선택해야합니다.(MALE: 1, FEMALE: 2)"),
    _INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST, "USERS4003", "유효하지 않은 리프레시 토큰입니다."),
    _EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "USERS4004", "인증 코드가 만료되었습니다."),
    // 401 Unauthorized
    _VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "USERS4011", "인증 코드 검증 실패"),
    _LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USERS4012", "이메일 주소 또는 비밀번호를 다시 확인하세요."),
    //404 Not Found
    _USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USERS4041", "사용자를 찾을 수 없습니다."),
    _USER_INACTIVE(HttpStatus.NOT_FOUND, "USERS4042", "사용자가 INACTIVE 임시 회원탈퇴 상태입니다."),
    //409 Conflict
    _DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USERS4091", "중복된 닉네임입니다."),
    _DUPLICATE_JOIN_REQUEST(HttpStatus.CONFLICT, "USERS4092", "중복된 이메일입니다."),
    // 인증 코드 전송실패
    _SEND_MAIL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "USERS5001", "인증 코드 전송 실패"),
    _REFRESH_TOKEN_SESSION_INVALID(HttpStatus.INTERNAL_SERVER_ERROR, "USERS5002", "리프레시 토큰 세션 정보가 올바르지 않습니다."),
    _REFRESH_TOKEN_PARSE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "USERS5003", "리프레시 토큰 세션 정보 파싱에 실패했습니다.");

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
