package com.umc.linkyou.jwt;

import com.umc.linkyou.apiPayload.code.status.auth.AuthErrorStatus;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtExceptionFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } catch (JwtException ex) {
            setErrorResponse(request, response, ex);
        }
    }

    public void setErrorResponse(HttpServletRequest req, HttpServletResponse res, Throwable ex)
            throws IOException {
        if (ex instanceof ExpiredJwtException) {
            SecurityErrorResponseWriter.write(res, AuthErrorStatus.EXPIRED_TOKEN);
            return;
        }
        SecurityErrorResponseWriter.write(res, AuthErrorStatus.INVALID_TOKEN);
    }
}
