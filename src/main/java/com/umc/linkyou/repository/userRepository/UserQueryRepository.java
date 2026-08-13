package com.umc.linkyou.repository.userRepository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QUsers;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.domain.mapping.folder.QUsersFolder;
import com.umc.linkyou.web.dto.user.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.umc.linkyou.domain.QAuthAccount;

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    public UserResponseDTO.UserProfileSummaryDto findUserProfileSummary(Long userId) {
        QUsers u = QUsers.users;
        QAuthAccount authAccount = QAuthAccount.authAccount;
        QUsersLinku ul = QUsersLinku.usersLinku;
        QUsersFolder uf = QUsersFolder.usersFolder;

        JPQLQuery<Long> linkCountSub = JPAExpressions.select(ul.count()).from(ul).where(ul.user.id.eq(u.id));
        JPQLQuery<Long> folderCountSub = JPAExpressions.select(uf.count()).from(uf).where(uf.user.id.eq(u.id));
        JPQLQuery<Long> aiLinkCountSub = JPAExpressions.select(ul.count()).from(ul).where(ul.user.id.eq(u.id).and(ul.aiExist.isTrue()));

        return queryFactory
                .select(Projections.constructor(
                        UserResponseDTO.UserProfileSummaryDto.class,
                        u.nickName,
                        authAccount.email,
                        u.gender,
                        u.job,
                        linkCountSub,
                        folderCountSub,
                        aiLinkCountSub
                ))
                .from(u)
                .leftJoin(authAccount).on(authAccount.user.eq(u))
                .where(u.id.eq(userId))
                .fetchFirst();
    }


}
