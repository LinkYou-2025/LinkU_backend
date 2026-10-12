package com.umc.linkyou.service.email;

import lombok.Getter;

@Getter
public class EmailSendException extends RuntimeException {

    private final Integer statusCode;
    private final String errorName;

    public EmailSendException(Integer statusCode, String errorName, Throwable cause) {
        super("Email provider request failed", cause);
        this.statusCode = statusCode;
        this.errorName = errorName;
    }
}
