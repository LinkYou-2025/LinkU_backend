package com.umc.linkyou.repository.authAccountRepository;


import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.AuthAccount;
import com.umc.linkyou.domain.QAuthAccount;
import com.umc.linkyou.domain.enums.Provider;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

@RequiredArgsConstructor
public class AuthAccountRepositoryImpl implements AuthAccountRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<AuthAccount> findByProviderAndExternalId(Provider provider, String externalId) {
        QAuthAccount authAccount = QAuthAccount.authAccount;

        AuthAccount result = queryFactory
                .selectFrom(authAccount)
                .where(
                        authAccount.provider.eq(provider),
                        authAccount.externalId.eq(externalId)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }
}