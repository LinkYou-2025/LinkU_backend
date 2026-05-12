package com.umc.linkyou.apiPayload.code.status.curation;

import com.umc.linkyou.apiPayload.code.BaseSuccessCode;
import com.umc.linkyou.apiPayload.code.SuccessReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CurationSuccessStatus implements BaseSuccessCode {

    CURATION_OK(HttpStatus.OK, "CURATION2001", "큐레이션 조회에 성공했습니다."),
    CURATION_LIST_OK(HttpStatus.OK, "CURATION2002", "큐레이션 목록 조회에 성공했습니다."),
    CURATION_SECTION_OK(HttpStatus.OK, "CURATION2003", "큐레이션 섹션 조회에 성공했습니다.");

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
