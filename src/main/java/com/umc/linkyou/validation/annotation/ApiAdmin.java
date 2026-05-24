package com.umc.linkyou.validation.annotation;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
@PreAuthorize("hasRole('ADMIN')") // Admin 권한 체크
public @interface ApiAdmin {//WebConfig를 통한 공통경로 설정이 되어 있음 /api/v1/admin
}
