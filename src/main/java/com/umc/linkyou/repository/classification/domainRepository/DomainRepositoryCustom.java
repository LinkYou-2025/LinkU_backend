package com.umc.linkyou.repository.classification.domainRepository;

import com.umc.linkyou.domain.classification.Domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DomainRepositoryCustom {
    Optional<Domain> findByDomainTail(String domainTail);
    Optional<Domain> findById(long l);
    List<Domain> findByDomainTailIn(Collection<String> domainTails);
    List<Domain> findDomainsCursorPaging(Long lastDomainId, int pageSize);
}
