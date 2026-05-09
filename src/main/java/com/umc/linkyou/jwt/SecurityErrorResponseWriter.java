package com.umc.linkyou.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class SecurityErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public void write(HttpServletResponse response, BaseErrorCode status) throws IOException {
        if (response.isCommitted()) {
            return;
        }

        ErrorReasonDTO reason = status.getReasonHttpStatus();

        response.setStatus(reason.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> body = ApiResponse.onFailure(
                reason.getCode(),
                reason.getMessage(),
                null
        );

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
