package com.umc.linkyou.repository.authAccountRepository;


import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.QAuthAccount;
import com.umc.linkyou.domain.QUsers;
import com.umc.linkyou.domain.Users;
import com.umc.linkyou.domain.enums.Provider;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

import static org.jsoup.select.Selector.select;

@RequiredArgsConstructor
public class AuthAccountRepositoryImpl implements AuthAccountRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private final QAuthAccount authAccount = QAuthAccount.authAccount;
    private final QUsers users = QUsers.users;

    @Override
    public Optional<AuthAccount> findByProviderAndExternalId(Provider provider, String externalId) {
        return Optional.ofNullable(queryFactory
                .selectFrom(authAccount)
                .where(authAccount.provider.eq(provider),
                        authAccount.externalId.eq(externalId))
                .fetchOne());
    }

    @Override
    public Optional<Long> findUserIdByEmailAndProvider(String email, Provider provider) {
        Long userId = queryFactory
                .select(authAccount.user.id)
                .from(authAccount)
                .where(authAccount.email.eq(email),
                        authAccount.provider.eq(provider))
                .fetchOne();
        return Optional.ofNullable(userId);
    }

    @Override
    public Optional<Users> findUserByEmailAndProvider(String email, Provider provider) {
        Users result = queryFactory
                .selectFrom(users)
                .leftJoin(users.authAccounts, authAccount)
                .where(authAccount.email.eq(email),
                        authAccount.provider.eq(provider))
                .fetchOne();
        return Optional.ofNullable(result);
    }
    @Override
    public Optional<Users> findUserByEmail(String email) {
        return Optional.ofNullable(queryFactory
                .select(authAccount.user)  // QAuthAccount.user 직접 select (private 무관)
                .from(authAccount)
                .where(authAccount.email.eq(email))
                .fetchOne());
    }

    @Override
    public Optional<Users> findUserByEmailExcludingProvider(String email, Provider provider) {
        return Optional.ofNullable(queryFactory
                .select(authAccount.user)
                .from(authAccount)
                .where(authAccount.email.eq(email)
                        .and(authAccount.provider.ne(provider)))  // 다른 provider만!
                .fetchFirst());
    }

    @Override
    public Optional<String> findEmailByUserIdAndProvider(Long userId, Provider provider) {
        return Optional.ofNullable(
                queryFactory
                        .select(authAccount.email)
                        .from(authAccount)
                        .where(authAccount.user.id.eq(userId)
                                .and(authAccount.provider.eq(provider)))
                        .fetchOne()
        );
    }
    @Override
    public boolean existsByUserIdAndProviderNot(Long userId, Provider provider) {
        Integer fetchOne = queryFactory
                .selectOne()
                .from(authAccount)
                .where(authAccount.user.id.eq(userId)
                        .and(authAccount.provider.ne(provider)))
                .fetchFirst();

        return fetchOne != null;
    }


}


