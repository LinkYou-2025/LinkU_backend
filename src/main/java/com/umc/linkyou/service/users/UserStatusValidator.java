package com.umc.linkyou.service.users;

import com.umc.linkyou.apiPayload.code.status.user.UserErrorStatus;
import com.umc.linkyou.apiPayload.exception.GeneralException;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class UserStatusValidator {

    // 탈퇴 유예 기간
    public static final int WITHDRAW_GRACE_PERIOD_DAYS = 14;

    public void validateLoginAllowed(Users user) {
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new GeneralException(UserErrorStatus._USER_INACTIVE);
        }
    }

    // 탈퇴 유예 기간 이내의 INACTIVE 유저인지 (복구 가능 여부)
    public boolean isWithinWithdrawGracePeriod(Users user) {
        if (user.getStatus() != UserStatus.INACTIVE || user.getInactiveDate() == null) {
            return false;
        }
        return ChronoUnit.DAYS.between(user.getInactiveDate(), LocalDateTime.now())
                <= WITHDRAW_GRACE_PERIOD_DAYS;
    }
}
