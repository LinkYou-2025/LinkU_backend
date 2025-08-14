package com.umc.linkyou.repository.categoryRepository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.folder.Fcolor;
import com.umc.linkyou.domain.folder.QFcolor;
import com.umc.linkyou.domain.mapping.folder.QUsersCategoryColor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class FcolorRepositoryImpl implements FcolorRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    @Override
    public Fcolor searchColorCode(Long fcolorId) {
        QFcolor fcolor = QFcolor.fcolor;

        // 색상 ID
        return queryFactory
                .selectFrom(fcolor)
                .where(fcolor.fcolorId.eq(fcolorId))
                .fetchOne();
    }
}