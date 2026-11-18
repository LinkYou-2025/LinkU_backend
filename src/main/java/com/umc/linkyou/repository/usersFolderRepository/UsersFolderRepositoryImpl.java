package com.umc.linkyou.repository.usersFolderRepository;

import org.springframework.stereotype.Repository;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UsersFolderRepositoryImpl implements UsersFolderRepositoryCustom {

    private final JPAQueryFactory queryFactory;
}
