package com.umc.linkyou.repository;

import com.umc.linkyou.domain.TermsAgreement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermsAgreementRepository extends JpaRepository<TermsAgreement, Long> {
    List<TermsAgreement> findByUserId(Long id);
}
