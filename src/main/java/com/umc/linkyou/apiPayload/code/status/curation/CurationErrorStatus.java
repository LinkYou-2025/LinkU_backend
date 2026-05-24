package com.umc.linkyou.apiPayload.code.status.curation;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CurationErrorStatus implements BaseErrorCode {

    // 403 Forbidden
    _CURATION_FORBIDDEN(HttpStatus.FORBIDDEN, "CURATION4031", "해당 큐레이션에 접근할 권한이 없습니다."),

    // 404 Not Found
    _CURATION_NOT_FOUND(HttpStatus.NOT_FOUND, "CURATION4041", "해당 큐레이션을 찾을 수 없습니다.");

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
