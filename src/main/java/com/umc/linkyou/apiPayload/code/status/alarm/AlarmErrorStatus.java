package com.umc.linkyou.apiPayload.code.status.alarm;


import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AlarmErrorStatus implements BaseErrorCode {

    ALARM_NOT_FOUND(HttpStatus.NOT_FOUND, "ALARM_NOT_FOUND", "알람을 찾을 수 없습니다."),
    ALARM_PERMISSION_DENIED(HttpStatus.UNAUTHORIZED, "ALARM_PERMISSION_DENIED", "알람에 대한 권한이 없습니다."),
    ALARM_TOPIC_SUBSCRIPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ALARM5001", "알림 주제 구독 상태 변경에 실패했습니다."),
    ALARM_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ALARM5002", "알림 전송에 실패했습니다.");



    private final HttpStatus statusCode;
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
                .httpStatus(statusCode)
                .build()
                ;
    }

}
