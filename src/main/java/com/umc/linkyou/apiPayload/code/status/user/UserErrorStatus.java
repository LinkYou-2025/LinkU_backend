package com.umc.linkyou.apiPayload.code.status.user;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorStatus implements BaseErrorCode {

    _LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "USERS4013", "이메일 주소 또는 비밀번호를 다시 확인하세요."),
    _DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USERS403", "중복된 닉네임입니다."),
    _DUPLICATE_JOIN_REQUEST(HttpStatus.CONFLICT, "USERS403", "중복된 이메일입니다."),
    _USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USERS404", "사용자를 찾을 수 없습니다."),
    _USER_INACTIVE(HttpStatus.NOT_FOUND, "USERS4042", "사용자가 INACTIVE 임시 회원탈퇴 상태입니다."),
    _INVALID_GENDER(HttpStatus.NOT_FOUND, "USERS404", "성별을 선택해야합니다.");

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
