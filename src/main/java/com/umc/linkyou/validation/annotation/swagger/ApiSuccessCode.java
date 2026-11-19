package com.umc.linkyou.validation.annotation.swagger;

import java.lang.annotation.*;

import com.umc.linkyou.apiPayload.code.status.SuccessStatus;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiSuccessCode {
    SuccessStatus value();
}
