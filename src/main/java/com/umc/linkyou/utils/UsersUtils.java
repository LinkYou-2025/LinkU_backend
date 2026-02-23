package com.umc.linkyou.utils;

import com.umc.linkyou.apiPayload.code.status.ErrorStatus;
import com.umc.linkyou.apiPayload.exception.handler.UserHandler;
import com.umc.linkyou.config.security.jwt.CustomUserDetails;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.repository.userRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsersUtils {
    private final UserRepository userRepository;

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
    //token이 올바른지, 사용자가 존재하는지, 사용자가 있다면 activated 상태인지
    public Users validateUser(CustomUserDetails userDetails) {
        if (userDetails == null) {
            throw new UserHandler(ErrorStatus._INVALID_TOKEN);
        }

        Long userId = userDetails.getUsers().getId();
        return userRepository.findNotInactiveUserById(userId)
                .orElseThrow(() -> {
                    if (!userRepository.existsById(userId)) {
                        return new UserHandler(ErrorStatus._USER_NOT_FOUND);
                    }
                    return new UserHandler(ErrorStatus._USER_INACTIVE);
                });
    }
}


