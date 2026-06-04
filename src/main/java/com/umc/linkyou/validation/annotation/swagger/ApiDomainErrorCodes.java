package com.umc.linkyou.validation.annotation.swagger;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDomainErrorCodes {
    Class<? extends BaseErrorCode>[] value();
}
