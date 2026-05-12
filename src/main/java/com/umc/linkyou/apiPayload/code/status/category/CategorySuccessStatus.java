package com.umc.linkyou.apiPayload.code.status.category;

import com.umc.linkyou.apiPayload.code.BaseSuccessCode;
import com.umc.linkyou.apiPayload.code.SuccessReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CategorySuccessStatus implements BaseSuccessCode {

    CATEGORY_OK(HttpStatus.OK, "CATEGORY2001", "카테고리 조회에 성공했습니다."),
    CATEGORY_COLOR_OK(HttpStatus.OK, "CATEGORY2002", "카테고리 색상 설정을 성공했습니다.");

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
