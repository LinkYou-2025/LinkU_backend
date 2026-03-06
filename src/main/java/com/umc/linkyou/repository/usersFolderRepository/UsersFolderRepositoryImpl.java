package com.umc.linkyou.repository.usersFolderRepository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UsersFolderRepositoryImpl implements UsersFolderRepositoryCustom {

    private final JPAQueryFactory queryFactory;
}
