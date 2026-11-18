package com.umc.linkyou.repository.categoryRepository;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.mapping.folder.QUsersCategoryColor;
import com.umc.linkyou.domain.mapping.folder.UsersCategoryColor;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UsersCategoryColorRepositoryImpl implements UsersCategoryColorRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public UsersCategoryColor searchCategoryColor(Long userId, Long categoryId) {
        QUsersCategoryColor usersCategoryColor = QUsersCategoryColor.usersCategoryColor;
        BooleanBuilder builder = new BooleanBuilder();

        // 사용자 ID
        if (userId != null) {
            builder.and(usersCategoryColor.user.id.eq(userId));
        }

        // 카테고리 ID
        if (categoryId != null) {
            builder.and(usersCategoryColor.category.categoryId.eq(categoryId));
        }

        return queryFactory.selectFrom(usersCategoryColor).where(builder).fetchOne();
    }
}
