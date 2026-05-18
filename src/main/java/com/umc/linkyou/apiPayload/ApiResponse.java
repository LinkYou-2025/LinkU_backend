package com.umc.linkyou.apiPayload;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.BaseSuccessCode;
import com.umc.linkyou.apiPayload.code.status.SuccessStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;

@Getter
@AllArgsConstructor
@JsonPropertyOrder({"isSuccess", "code", "message", "timestamp", "result"})
public class ApiResponse<T> {

    @Schema(description = "성공 여부")
    @JsonProperty("isSuccess")
    private final Boolean isSuccess;

    @Schema(description = "응답 코드")
    private final String code;

    @Schema(description = "응답 메시지")
    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final LocalDateTime timestamp;

    @Schema(type = "object")
    private final T result;

    // Code, result 모두 커스텀
    public static <T> ApiResponse<T> onSuccess(BaseSuccessCode code, T result) {
        return success(code.getReason().getCode(), code.getReason().getMessage(), result);
    }

    // result가 비어 있을 경우
    public static ApiResponse<Object> onSuccess(BaseSuccessCode code) {
        return success(code.getReason().getCode(), code.getReason().getMessage(), Collections.emptyMap());
    }

    // 기본 SUCCESS, result 포함
    public static <T> ApiResponse<T> onSuccess(T result) {
        return success(SuccessStatus._OK.getReason().getCode(), SuccessStatus._OK.getReason().getMessage(), result);
    }

    private static <T> ApiResponse<T> success(String code, String message, T result) {
        return new ApiResponse<>(true, code, message, LocalDateTime.now(), result);
    }

    private static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(false, code, message, LocalDateTime.now(), data);
    }

    public static <T> ApiResponse<T> onFailure(BaseErrorCode code, T data) {
        return failure(code.getReason().getCode(), code.getReason().getMessage(), data);
    }

    public static <T> ApiResponse<T> onFailure(String code, String message, T data) {
        return failure(code, message, data);
    }
}
