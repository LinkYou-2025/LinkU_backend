package com.umc.linkyou.validation.annotation.swagger;

import com.umc.linkyou.apiPayload.code.status.auth.AuthSuccessStatus;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiAuthSuccessCode {
    AuthSuccessStatus value();
}
