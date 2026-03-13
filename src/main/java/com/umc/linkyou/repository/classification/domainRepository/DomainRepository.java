package com.umc.linkyou.repository.classification.domainRepository;

import com.umc.linkyou.domain.classification.Domain;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DomainRepository extends JpaRepository<Domain, Long>, DomainRepositoryCustom {

}
