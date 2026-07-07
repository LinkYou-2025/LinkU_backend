package com.umc.linkyou.apiPayload.code.status.alarm;

import com.umc.linkyou.apiPayload.code.BaseSuccessCode;
import com.umc.linkyou.apiPayload.code.SuccessReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AlarmSuccessStatus implements BaseSuccessCode {

    FCM_TOKEN_REGISTERED(HttpStatus.OK, "ALARM2001", "FCM 토큰이 정상적으로 등록되었습니다."),
    FCM_TOKEN_DELETED(HttpStatus.OK, "ALARM2002", "FCM 토큰이 정상적으로 삭제되었습니다."),
    ALARM_TEST_SENT(HttpStatus.OK, "ALARM2003", "테스트 알림이 정상적으로 전송되었습니다."),
    ALARM_SETTING_OK(HttpStatus.OK, "ALARM2004", "알림 설정 조회에 성공했습니다."),
    ALARM_SETTING_UPDATED(HttpStatus.OK, "ALARM2005", "알림 설정이 정상적으로 수정되었습니다."),
    ALARM_LIST_OK(HttpStatus.OK, "ALARM2006", "알림 목록 조회에 성공했습니다."),
    ALARM_DETAIL_OK(HttpStatus.OK, "ALARM2007", "알림 상세 조회에 성공했습니다."),
    ALARM_BROADCAST_REGISTERED(HttpStatus.OK, "ALARM2008", "관리자 브로드캐스트 알림이 정상적으로 등록되었습니다."),
    ALARM_READ(HttpStatus.OK, "ALARM2009", "알림이 읽음 처리되었습니다."),
    ALARM_UNREAD_EXISTS_OK(HttpStatus.OK, "ALARM2010", "읽지 않은 알림 존재 여부 조회에 성공했습니다.");

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
