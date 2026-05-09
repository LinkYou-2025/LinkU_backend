package com.umc.linkyou.apiPayload.code.status.aiarticle;

import com.umc.linkyou.apiPayload.code.BaseSuccessCode;
import com.umc.linkyou.apiPayload.code.SuccessReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AiArticleSuccessStatus implements BaseSuccessCode {

    AI_ARTICLE_OK(HttpStatus.OK, "AIARTICLE2001", "AI 아티클을 가져왔습니다."),
    AI_ARTICLE_LIST_OK(HttpStatus.OK, "AIARTICLE2002", "AI 아티클 목록을 가져왔습니다.");

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
