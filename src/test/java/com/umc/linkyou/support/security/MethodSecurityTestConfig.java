package com.umc.linkyou.support.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import com.umc.linkyou.jwt.CustomAccessDeniedHandler;
import com.umc.linkyou.jwt.SecurityErrorResponseWriter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// @code @ApiAdmin}/{@code @ApiManager}처럼 {@code @PreAuthorize}로 권한을 제한하는 컨트롤러를
// {@code @WebMvcTest}에서 검증할 때 {@link TestSecurityConfig} 대신 사용한다.
@TestConfiguration
@EnableMethodSecurity
public class MethodSecurityTestConfig {

    @Bean
    public SecurityErrorResponseWriter securityErrorResponseWriter(ObjectMapper objectMapper) {
        return new SecurityErrorResponseWriter(objectMapper);
    }

    @Bean
    public CustomAccessDeniedHandler customAccessDeniedHandler(SecurityErrorResponseWriter writer) {
        return new CustomAccessDeniedHandler(writer);
    }

    @Bean
    @Order(0)
    public SecurityFilterChain methodSecurityTestFilterChain(
            HttpSecurity http,
            SecurityErrorResponseWriter securityErrorResponseWriter,
            CustomAccessDeniedHandler accessDeniedHandler) throws Exception {
        http.securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                securityErrorResponseWriter.write(response, AuthErrorStatus.UNAUTHORIZED))
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll());

        return http.build();
    }
}
