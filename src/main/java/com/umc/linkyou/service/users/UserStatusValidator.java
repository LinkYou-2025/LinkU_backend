package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserStatusValidator {

    public void validateLoginAllowed(Users user) {
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new GeneralException(UserErrorStatus._USER_INACTIVE);
        }
    }
}
