package com.umc.linkyou.batch.user;

import com.umc.linkyou.domain.Users;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 비활성 사용자 정리 정책을 적용하는 프로세서
 */
@Component
public class InactiveUserItemProcessor implements ItemProcessor<Users, Users> {

    private static final int GRACE_PERIOD_DAYS = 14;

    @Override
    public Users process(Users user) {
        if (user.getInactiveDate() == null) {
            return null;
        }

        if (user.getInactiveDate().isAfter(LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS))) {
            return null;
        }

        return user;
    }
}
