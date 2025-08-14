package com.umc.linkyou.repository.classification.domainRepository;

import com.umc.linkyou.domain.classification.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface DomainRepository extends JpaRepository<Domain, Long>, DomainRepositoryCustom {

}
