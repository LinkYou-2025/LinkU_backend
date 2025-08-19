package com.umc.linkyou.repository.userRepository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QAiArticle;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.QUsers;
import com.umc.linkyou.domain.classification.Job;
import com.umc.linkyou.domain.enums.Gender;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.domain.mapping.folder.QUsersFolder;
import com.umc.linkyou.web.dto.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory queryFactory;

    public UserResponseDTO.UserProfileSummaryDto findUserProfileSummary(Long userId) {
        QUsers u = QUsers.users;
        QUsersLinku ul = QUsersLinku.usersLinku;
        QUsersFolder uf = QUsersFolder.usersFolder;
        QLinku l = QLinku.linku1;
        QAiArticle a = QAiArticle.aiArticle;

        JPQLQuery<Long> linkCountSub = JPAExpressions
                .select(ul.count())
                .from(ul)
                .where(ul.user.id.eq(u.id));

        JPQLQuery<Long> folderCountSub = JPAExpressions
                .select(uf.count())
                .from(uf)
                .where(uf.user.id.eq(u.id));

        JPQLQuery<Long> aiLinkCountSub = JPAExpressions
                .select(a.count())
                .from(ul)
                .join(ul.linku, l)
                .join(l.aiArticle, a)
                .where(ul.user.id.eq(u.id));

        return queryFactory
                .select(Projections.constructor(
                        UserResponseDTO.UserProfileSummaryDto.class,
                        u.nickName,
                        u.email,
                        u.gender,
                        u.job,
                        linkCountSub,
                        folderCountSub,
                        aiLinkCountSub
                ))
                .from(u)
                .where(u.id.eq(userId))
                .fetchOne();
    }

}