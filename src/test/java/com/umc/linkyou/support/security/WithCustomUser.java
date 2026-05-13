package com.umc.linkyou.support.security;

import com.umc.linkyou.domain.enums.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@WithSecurityContext(factory = WithCustomUserSecurityContextFactory.class)
public @interface WithCustomUser {

    long userId() default 1L;

    String nickName() default "linku-user";

    Role role() default Role.USER;

    String provider() default "kakao";
}
