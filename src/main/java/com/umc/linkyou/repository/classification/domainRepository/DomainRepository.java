package com.umc.linkyou.repository.classification.domainRepository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.umc.linkyou.domain.classification.Domain;

public interface DomainRepository extends JpaRepository<Domain, Long>, DomainRepositoryCustom {}
