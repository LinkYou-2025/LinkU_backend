package com.umc.linkyou.repository.classification;

import com.umc.linkyou.domain.classification.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long> {
    Optional<Domain> findByDomainTail(String domainTail);

    Optional<Domain> findById(long l);

    // N+1 제거용 일괄 조회
    List<Domain> findByDomainTailIn(Collection<String> domainTails);
}
