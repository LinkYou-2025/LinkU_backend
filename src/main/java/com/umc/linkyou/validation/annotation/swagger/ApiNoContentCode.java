package com.umc.linkyou.validation.annotation.swagger;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiNoContentCode {
    String description() default "본문 없이 204 No Content를 반환합니다.";
}
