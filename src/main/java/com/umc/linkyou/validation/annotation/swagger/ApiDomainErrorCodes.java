package com.umc.linkyou.validation.annotation.swagger;

import java.lang.annotation.*;

import com.umc.linkyou.apiPayload.code.BaseErrorCode;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiDomainErrorCodes {
    Class<? extends BaseErrorCode>[] value();
}
