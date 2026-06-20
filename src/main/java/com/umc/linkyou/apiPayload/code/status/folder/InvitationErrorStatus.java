package com.umc.linkyou.apiPayload.code.status.folder;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum InvitationErrorStatus implements BaseErrorCode {

    // 403 Forbidden
    INVITATION_CREATOR_CANNOT_ACCEPT(HttpStatus.FORBIDDEN, "INVITATION4031", "초대 생성자는 자신의 링크로 참여할 수 없습니다."),

    // 404 Not Found
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "INVITATION4041", "공유 폴더 토큰을 찾을 수 없습니다."),
    INVITATION_EXPIRED(HttpStatus.NOT_FOUND, "INVITATION4042", "공유 폴더 토큰이 유효하지 않습니다."),
    INVITATION_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "INVITATION4043", "공유 폴더 링크가 유효하지 않습니다.");

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
