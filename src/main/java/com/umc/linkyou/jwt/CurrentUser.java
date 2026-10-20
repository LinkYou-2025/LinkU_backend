package com.umc.linkyou.jwt;

import java.lang.annotation.*;

import io.swagger.v3.oas.annotations.Parameter;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Parameter(hidden = true)
public @interface CurrentUser {
    boolean required() default true;
}
