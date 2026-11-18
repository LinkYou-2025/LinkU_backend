package com.umc.linkyou.validation.annotation;

import java.lang.annotation.*;

import org.springframework.web.bind.annotation.RestController;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
public @interface ApiV2 {}
