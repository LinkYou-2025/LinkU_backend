package com.umc.linkyou.repository.usersLinkuRepository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.domain.mapping.UsersLinku;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UsersLinkuRepositoryImpl implements UsersLinkuRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UsersLinku> findRecentLinkCandidatesByUser(Long userId, int limit) {
        QUsersLinku usersLinku = QUsersLinku.usersLinku;

        return queryFactory
                .selectFrom(usersLinku)
                .join(usersLinku.linku).fetchJoin()
                .where(
                        usersLinku.user.id.eq(userId),
                        usersLinku.createdAt.after(LocalDateTime.now().minusMonths(1))
                )
                .orderBy(usersLinku.createdAt.desc())
                .limit(limit)
                .fetch();
    }
}