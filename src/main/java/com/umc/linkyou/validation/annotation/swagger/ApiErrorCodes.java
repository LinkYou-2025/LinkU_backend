package com.umc.linkyou.validation.annotation.swagger;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiErrorCodes {
    ApiErrorCode[] value();
}
