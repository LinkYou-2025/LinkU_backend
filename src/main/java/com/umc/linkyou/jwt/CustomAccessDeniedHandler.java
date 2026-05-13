package com.umc.linkyou.jwt;

import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        securityErrorResponseWriter.write(response, AuthErrorStatus.PERMISSION_DENIED);
    }
}
