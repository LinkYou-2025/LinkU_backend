package com.umc.linkyou.repository.classification.domainRepository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.umc.linkyou.domain.classification.Domain;
import com.umc.linkyou.domain.classification.QDomain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DomainRepositoryImpl implements DomainRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QDomain d = QDomain.domain;

    @Override
    public Optional<Domain> findByDomainTail(String domainTail) {
        Domain result = queryFactory
                .selectFrom(d)
                .where(d.domainTail.eq(domainTail))
                .fetchFirst();
        return Optional.ofNullable(result);
    }
    @Override
    public Optional<Domain> findById(long id) {
        Domain result = queryFactory
                .selectFrom(d)
                .where(d.domainId.eq(id))
                .fetchFirst();
        return Optional.ofNullable(result);
    }
    @Override
    public List<Domain> findByDomainTailIn(Collection<String> domainTails) {
        return queryFactory
                .selectFrom(d)
                .where(d.domainTail.in(domainTails))
                .fetch();
    }
    @Override
    public List<Domain> findDomainsCursorPaging(Long lastDomainId, int pageSize) {
        return queryFactory
                .selectFrom(d)
                .where(lastDomainId != null ? d.domainId.lt(lastDomainId) : null)
                .orderBy(d.domainId.desc())
                .limit(pageSize)
                .fetch();
    }
}
