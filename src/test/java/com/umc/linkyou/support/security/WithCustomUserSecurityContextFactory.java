package com.umc.linkyou.support.security;

import com.umc.linkyou.domain.Users;
import com.umc.linkyou.jwt.CustomUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.test.util.ReflectionTestUtils;

public class WithCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithCustomUser> {

    @Override
    public SecurityContext createSecurityContext(WithCustomUser annotation) {
        Users user = Users.builder()
                .nickName(annotation.nickName())
                .password("password")
                .role(annotation.role())
                .build();
        ReflectionTestUtils.setField(user, "id", annotation.userId());

        CustomUserDetails principal = new CustomUserDetails(user, annotation.provider());

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        return context;
    }
}
