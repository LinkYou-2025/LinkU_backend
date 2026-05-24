package com.umc.linkyou.apiPayload.code.status.auth;

import com.umc.linkyou.apiPayload.code.BaseSuccessCode;
import com.umc.linkyou.apiPayload.code.SuccessReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthSuccessStatus implements BaseSuccessCode {

    JOIN_SUCCESS(HttpStatus.CREATED, "AUTH201", "회원가입이 완료되었습니다."),
    LOGIN_SUCCESS(HttpStatus.OK, "AUTH2001", "로그인 성공"),
    LOGOUT_SUCCESS(HttpStatus.NO_CONTENT, "AUTH204", "로그아웃 성공"),
    TOKEN_REISSUE_SUCCESS(HttpStatus.OK, "AUTH2002", "토큰이 재발급되었습니다."),
    SEND_VERIFICATION_CODE(HttpStatus.ACCEPTED, "AUTH2003", "인증 코드가 전송되었습니다."),
    EMAIL_VERIFICATION_SUCCESS(HttpStatus.OK, "AUTH2004", "이메일 인증이 완료되었습니다."),
    NICKNAME_AVAILABLE(HttpStatus.OK, "AUTH2005", "사용 가능한 닉네임입니다."),
    PASSWORD_RESET_LINK_SENT(HttpStatus.OK, "AUTH2006", "비밀번호 재설정 링크가 전송되었습니다."),
    PASSWORD_RESET_SUCCESS(HttpStatus.OK, "AUTH2007", "비밀번호가 재설정되었습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    @Override
    public SuccessReasonDTO getReason() {
        return SuccessReasonDTO.builder().message(message).code(code).isSuccess(true).build();
    }

    @Override
    public SuccessReasonDTO getReasonHttpStatus() {
        return SuccessReasonDTO.builder().message(message).code(code).isSuccess(true).httpStatus(httpStatus).build();
    }
}

