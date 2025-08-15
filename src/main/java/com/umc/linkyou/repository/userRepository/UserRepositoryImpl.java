package com.umc.linkyou.repository.userRepository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QUsers;
import com.umc.linkyou.domain.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QUsers users = QUsers.users;

    @Override
    public List<Users> findAllByStatusAndInactiveDateBefore(String status, LocalDateTime beforeDateTime) {
        return queryFactory.selectFrom(users)
                .where(users.status.eq(status)
                        .and(users.inactiveDate.before(beforeDateTime)))
                .fetch();
    }
}
