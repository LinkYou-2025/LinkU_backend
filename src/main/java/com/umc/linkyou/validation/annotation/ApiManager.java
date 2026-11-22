package com.umc.linkyou.validation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController // 매니저 API 공통 경로 /api/v1/manage
@PreAuthorize("hasRole('MANAGER')")
public @interface ApiManager {}
