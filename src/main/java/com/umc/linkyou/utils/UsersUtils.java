package com.umc.linkyou.utils;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import org.springframework.stereotype.Component;

@Component
public class UsersUtils {

    public Long getAuthenticatedUserId(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UserHandler(ErrorStatus._INVALID_TOKEN);
        }
        return userDetails.getUsers().getId();
    }
    public String getAuthenticatedUserEmail(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UserHandler(ErrorStatus._INVALID_TOKEN);
        }
        return userDetails.getEmail();
    }
}


