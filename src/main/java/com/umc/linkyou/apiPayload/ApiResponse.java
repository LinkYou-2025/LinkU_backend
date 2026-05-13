package com.umc.linkyou.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.linkyou.apiPayload.code.BaseCode;
import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.BaseSuccessCode;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "timestamp", "result"})
public class ApiResponse<T> {

    @Schema(description = "성공 여부", example = "true")
    @JsonProperty("isSuccess")
    private final Boolean isSuccess;

    @Schema(description = "응답 코드", example = "COMMON200")
    private final String code;

    @Schema(description = "응답 메시지", example = "성공입니다.")
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final LocalDateTime timestamp;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;


    // 성공한 경우 응답 생성
    public static <T> ApiResponse<T> of(BaseCode code, T result) {
        var reason = code.getReasonHttpStatus();
        return new ApiResponse<>(true, reason.getCode(), reason.getMessage(), LocalDateTime.now(), result);
    }

    public static <T> ApiResponse<T> of(BaseSuccessCode code, T result) {
        var reason = code.getReasonHttpStatus();
        return new ApiResponse<>(true, reason.getCode(), reason.getMessage(), LocalDateTime.now(), result);
    }


    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T result) {
        return new ApiResponse<>(true, code.getReason().getCode(), code.getReason().getMessage(), LocalDateTime.now(), result);
    }

    public static <T> ApiResponse<T> onSuccess(T result) {
        return of(SuccessStatus._OK, result);
    }


    // 실패한 경우 응답 생성 - 해당 메서드 사용 권장
    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T data){
        return new ApiResponse<>(false, code.getReason().getCode(), code.getReason().getMessage(), LocalDateTime.now(), data);
    }

    // 실패한 경우 응답 생성
    public static <T> ApiResponse<T> onFailure(String code, String message, T data){
        return new ApiResponse<>(false, code, message, LocalDateTime.now(), data);
    }
}
