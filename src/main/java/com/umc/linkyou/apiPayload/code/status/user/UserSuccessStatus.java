package com.umc.linkyou.apiPayload.code.status.user;

import com.umc.linkyou.apiPayload.code.BaseSuccessCode;
import com.umc.linkyou.apiPayload.code.SuccessReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserSuccessStatus implements BaseSuccessCode {

    USER_INFO_OK(HttpStatus.OK, "USERS2001", "사용자 정보 조회에 성공했습니다."),
    USER_PROFILE_UPDATED(HttpStatus.OK, "USERS2002", "마이페이지가 수정되었습니다."),
    USER_WITHDRAW_OK(HttpStatus.OK, "USERS2003", "회원탈퇴가 완료되었습니다."),
    SOCIAL_PROFILE_COMPLETED(HttpStatus.OK, "USERS2004", "소셜 프로필 설정이 완료되었습니다."),
    TERMS_BATCH_OK(HttpStatus.OK, "USERS2005", "약관 일괄 동의가 완료되었습니다."),
    TERMS_UPDATE_OK(HttpStatus.OK, "USERS2006", "약관 동의 상태가 수정되었습니다."),
    TERMS_STATUS_OK(HttpStatus.OK, "USERS2007", "약관 동의 상태 조회에 성공했습니다."),
    USER_NICKNAME_OK(HttpStatus.OK, "USERS2008", "닉네임 조회에 성공했습니다.");

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
