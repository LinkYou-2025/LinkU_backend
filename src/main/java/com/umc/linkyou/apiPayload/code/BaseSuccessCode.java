package com.umc.linkyou.apiPayload.code;

public interface BaseSuccessCode {
    SuccessReasonDTO getReason();

    SuccessReasonDTO getReasonHttpStatus();
}
