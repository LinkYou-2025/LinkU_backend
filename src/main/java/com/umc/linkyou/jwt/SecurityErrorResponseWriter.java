package com.umc.linkyou.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.umc.linkyou.apiPayload.ApiResponse;
import com.umc.linkyou.apiPayload.code.BaseErrorCode;
import com.umc.linkyou.apiPayload.code.ErrorReasonDTO;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public static void write(HttpServletResponse response, BaseErrorCode status) throws IOException {
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

        OBJECT_MAPPER.writeValue(response.getOutputStream(), body);
    }
}
