package com.umc.linkyou.validation.annotation.swagger;

import com.umc.linkyou.apiPayload.code.status.AiArticleErrorStatus;
import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.code.status.alarm.AlarmErrorStatus;
import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ApiErrorCodes.class)
public @interface ApiErrorCode {
    ErrorStatus[] errorStatus() default {};      // 공통 에러
    UserErrorStatus[] userErrorStatus() default {}; // 유저 에러
    AlarmErrorStatus[] alarmErrorStatus() default {}; // 유저 에러
    AiArticleErrorStatus[] aiArticleErrorStatus() default {};
}
