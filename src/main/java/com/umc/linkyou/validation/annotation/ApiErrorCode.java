package com.umc.linkyou.validation.annotation;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCode {
    ErrorStatus[] value();
}
