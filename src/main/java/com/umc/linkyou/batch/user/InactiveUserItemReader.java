package com.umc.linkyou.batch.user;

import com.umc.linkyou.batch.common.QuerydslPagingItemReader;
import com.umc.linkyou.domain.QUsers;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.UserStatus;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 비활성 사용자 정리 대상 reader
 */
@Slf4j
@Component
public class InactiveUserItemReader extends QuerydslPagingItemReader<Users> {

    private static final int GRACE_PERIOD_DAYS = 14;

    public InactiveUserItemReader(EntityManagerFactory entityManagerFactory) {
        super("inactiveUserItemReader", 100, entityManagerFactory);
    }

    @Override
    protected List<Users> fetchQuery(Long lastId, int pageSize) {
        QUsers users = QUsers.users;
        LocalDateTime cutoffDateTime = LocalDateTime.now().minusDays(GRACE_PERIOD_DAYS);

        return queryFactory.selectFrom(users)
                .where(
                        users.id.gt(lastId),
                        users.status.eq(UserStatus.INACTIVE),
                        users.inactiveDate.isNotNull(),
                        users.inactiveDate.loe(cutoffDateTime)
                )
                .orderBy(users.id.asc())
                .limit(pageSize)
                .fetch();
    }

    @Override
    protected Long getLastId(Users item) {
        return item.getId();
    }
}
