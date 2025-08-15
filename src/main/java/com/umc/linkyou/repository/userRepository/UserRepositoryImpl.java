package com.umc.linkyou.repository.userRepository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.QAiArticle;
import com.umc.linkyou.domain.QLinku;
import com.umc.linkyou.domain.QUsers;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.classification.QJob;
import com.umc.linkyou.domain.folder.QFolder;
import com.umc.linkyou.domain.mapping.QUsersLinku;
import com.umc.linkyou.domain.mapping.folder.QUsersFolder;
import com.umc.linkyou.web.dto.UserResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private final QUsers users = QUsers.users;
    private final QUsersLinku usersLinku = QUsersLinku.usersLinku;
    private final QUsersFolder usersFolder = QUsersFolder.usersFolder;
    private final QLinku linku = QLinku.linku1;
    private final QAiArticle aiArticle = QAiArticle.aiArticle;
    private final QFolder folder = QFolder.folder;
    private final QJob job = QJob.job;

    @Override
    public UserResponseDTO.UserInfoDTO findUserWithFoldersAndLinks(Long userId) {
        return queryFactory.select(Projections.constructor(
                        UserResponseDTO.UserInfoDTO.class,
                        users.nickName,
                        users.email,
                        users.gender,
                        users.job,
                        // 링크 개수: UsersLinku에서 userId 조건으로 countDistinct linkuId
                        queryFactory.select(usersLinku.linku.linkuId.countDistinct())
                                .from(usersLinku)
                                .where(usersLinku.user.id.eq(userId)),
                        // 폴더 개수: UsersFolder에서 userId 조건으로 countDistinct folderId
                        queryFactory.select(usersFolder.folder.folderId.countDistinct())
                                .from(usersFolder)
                                .where(usersFolder.user.id.eq(userId)),
                        // AI 링크 개수: UsersLinku → linku → aiArticle 조건 (title not null and not empty)
                        queryFactory.select(usersLinku.linku.aiArticle.countDistinct())
                                .from(usersLinku)
                                .where(usersLinku.user.id.eq(userId)
                                        .and(usersLinku.linku.aiArticle.title.isNotNull())
                                        .and(usersLinku.linku.aiArticle.title.isNotEmpty()))
                ))
                .from(users)
                .where(users.id.eq(userId))
                .fetchOne();
    }

    @Override
    public List<Users> findAllByStatusAndInactiveDateBefore(String status, LocalDateTime beforeDateTime) {
        return queryFactory.selectFrom(users)
                .where(users.status.eq(status)
                        .and(users.inactiveDate.before(beforeDateTime)))
                .fetch();
    }
}
